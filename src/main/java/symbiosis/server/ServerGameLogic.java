package symbiosis.server;

import symbiosis.common.model.*;
import symbiosis.common.net.*;

import java.util.List;

public class ServerGameLogic {

    private final GameServer server;
    private GameState gameState;

    private ClientHandler fishClient;
    private ClientHandler crabClient;

    private final String[][] levels;
    private int currentLevelIndex = 0;

    private Integer fishVote = null;
    private Integer crabVote = null;

    private boolean restartRequested = false;
    private ClientHandler restartRequester = null;


    public ServerGameLogic(GameServer server) {
        this.server = server;

        String[] level0 = new String[]{
                "############",
                "#..........#",
                "#..D....L..#",
                "#..####....#",
                "#..#..M....#",
                "#..#..B....#",
                "#......E...#",
                "############"
        };

        String[] level1 = new String[]{
                "############",
                "#..M....B..#",
                "#..####....#",
                "#..D.......#",
                "#......L...#",
                "#..B....M..#",
                "#...E......#",
                "############"
        };

        String[] level2 = new String[]{
                "############",
                "#..D....M..#",
                "#..####....#",
                "#..B....L..#",
                "#..#..B....#",
                "#..#....M..#",
                "#...E......#",
                "############"
        };

        String[] level3 = new String[]{
                "############",
                "#..M....B..#",
                "###.####...#",
                "#..D....L..#",
                "#..B..M....#",
                "#..####....#",
                "#E.......B.#",
                "############"
        };

        String[] level4 = new String[]{
                "############",
                "#M...B..D..#",
                "#.####.###.#",
                "#...L..M...#",
                "#.B..###...#",
                "#...B...M..#",
                "#..E.......#",
                "############"
        };

        String[] level5 = new String[]{
                "############",
                "#..M....B..#",
                "#.####.###.#",
                "#..D....L..#",
                "#..B..M....#",
                "#.####.###.#",
                "#....B.....#",
                "#..M....B..#",
                "#...E......#",
                "############"
        };

        this.levels = new String[][]{
                level0, level1, level2, level3, level4, level5
        };

        this.gameState = loadLevel(0);
    }

    private TileType charToTile(char c) {
        return switch (c) {
            case '#' -> TileType.WALL;
            case 'E' -> TileType.EXIT;
            case 'D' -> TileType.DARK_TILE;
            case 'L' -> TileType.LIGHT_TILE;
            default -> TileType.EMPTY;
        };
    }

    private GameState loadLevel(int levelIndex) {
        String[] rows = levels[levelIndex];
        int height = rows.length;
        int width = rows[0].length();

        CaveMap map = new CaveMap(width, height);

        for (int y = 0; y < height; y++) {
            String row = rows[y];
            for (int x = 0; x < width; x++) {
                char c = row.charAt(x);
                map.setTile(x, y, charToTile(c));
            }
        }

        GameState state = new GameState(map);
        state.setLevelCompleted(false);

        for (int y = 0; y < height; y++) {
            String row = rows[y];
            for (int x = 0; x < width; x++) {
                char c = row.charAt(x);
                switch (c) {
                    case 'M' -> state.getObjects()
                            .add(new GameObject(ObjectType.MUSHROOM, new Position(x, y)));
                    case 'B' -> state.getObjects()
                            .add(new GameObject(ObjectType.BOX, new Position(x, y)));
                }
            }
        }
        return state;
    }

    private void placePlayersForCurrentLevel() {
        if (gameState.getFish() != null) {
            Position fishPos = new Position(1, 1);
            gameState.getFish().setPosition(fishPos);
        }

        if (gameState.getCrab() != null) {
            Position crabPos;

            if (currentLevelIndex == 0) {
                crabPos = new Position(2, 5);
            } else if (currentLevelIndex == 1) {
                crabPos = new Position(10, 5);
            } else if (currentLevelIndex == 2) {
                crabPos = new Position(2, 6);
            } else if (currentLevelIndex == 3) {
                crabPos = new Position(9, 6);
            } else if (currentLevelIndex == 4) {
                crabPos = new Position(2, 6);
            } else if (currentLevelIndex == 5) {
                crabPos = new Position(9, 7);
            } else {
                crabPos = new Position(2, 5);
            }

            gameState.getCrab().setPosition(crabPos);
        }
    }

    public synchronized void handleJoin(ClientHandler handler, JoinMessage msg) {
        String name = msg.getPlayerName();

        if (fishClient == null && crabClient == null) {
            int prefLevel = msg.getPreferredLevel();
            if (prefLevel >= 0 && prefLevel < levels.length) {
                currentLevelIndex = prefLevel;
                this.gameState = loadLevel(currentLevelIndex);
            }
        }

        String prefRoleStr = msg.getPreferredRole();
        PlayerRole requested = null;
        if (prefRoleStr != null) {
            if ("FISH".equalsIgnoreCase(prefRoleStr)) {
                requested = PlayerRole.FISH;
            } else if ("CRAB".equalsIgnoreCase(prefRoleStr)) {
                requested = PlayerRole.CRAB;
            }
        }

        PlayerRole assignedRole;

        if (requested == PlayerRole.FISH && fishClient == null) {
            assignedRole = PlayerRole.FISH;
            fishClient = handler;
        } else if (requested == PlayerRole.CRAB && crabClient == null) {
            assignedRole = PlayerRole.CRAB;
            crabClient = handler;
        } else {
            if (fishClient == null) {
                assignedRole = PlayerRole.FISH;
                fishClient = handler;
            } else if (crabClient == null) {
                assignedRole = PlayerRole.CRAB;
                crabClient = handler;
            } else {
                handler.send(new ErrorMessage("FULL", "Server already has two players"));
                return;
            }
        }

        if (assignedRole == PlayerRole.FISH) {
            Player fish = new Player(handler.getClientId(), name, assignedRole, new Position(1, 1));
            gameState.setFish(fish);
        } else {
            Player crab = new Player(handler.getClientId(), name, assignedRole, new Position(2, 5));
            gameState.setCrab(crab);
        }

        placePlayersForCurrentLevel();

        handler.send(new RoleAssignedMessage(handler.getClientId(), assignedRole.name()));

        handler.send(new LevelDataMessage(
                gameState.getMap().getWidth(),
                gameState.getMap().getHeight(),
                levels[currentLevelIndex]
        ));

        broadcastState();
    }

    public synchronized void handleInput(InputMessage msg) {
        Player p = findPlayerById(msg.getClientId());
        if (p == null) return;

        if (msg.getInputType() == InputMessage.InputType.ACTION && gameState.isLevelCompleted()) {
            handlePostWinAction(p);
            return;
        }

        Position pos = p.getPosition();
        int x = pos.getX();
        int y = pos.getY();

        int dx = 0;
        int dy = 0;

        switch (msg.getInputType()) {
            case MOVE_UP -> dy = -1;
            case MOVE_DOWN -> dy = 1;
            case MOVE_LEFT -> dx = -1;
            case MOVE_RIGHT -> dx = 1;
            case ACTION -> {
                if (p.getRole() == PlayerRole.FISH) {
                    activateMushroomAt(pos.getX(), pos.getY());
                    checkLevelCompleted();
                    broadcastState();
                }
                return;
            }
            case STOP -> {
                return;
            }
        }

        if (dx == 0 && dy == 0) return;

        int targetX = x + dx;
        int targetY = y + dy;

        GameObject boxAtTarget = findObjectAt(targetX, targetY, ObjectType.BOX);

        if (boxAtTarget != null && p.getRole() == PlayerRole.CRAB) {
            int boxTargetX = targetX + dx;
            int boxTargetY = targetY + dy;

            if (isWalkable(boxTargetX, boxTargetY)
                    && !isObjectBlocking(boxTargetX, boxTargetY)) {
                boxAtTarget.setPosition(new Position(boxTargetX, boxTargetY));
                p.setPosition(new Position(targetX, targetY));
            }
        } else {
            if (isWalkable(targetX, targetY) && !isObjectBlocking(targetX, targetY)) {
                p.setPosition(new Position(targetX, targetY));
                if (p.getRole() == PlayerRole.FISH) {
                    activateMushroomAt(targetX, targetY);
                }
            }
        }

        checkLevelCompleted();
        broadcastState();
    }

    public synchronized void handleChat(ChatMessage msg) {
        broadcast(msg);
    }

    public synchronized void handleLevelVote(ClientHandler handler, LevelVoteMessage vote) {
        int idx = vote.getLevelIndex();

        if (!gameState.isLevelCompleted()) {
            handler.send(new ErrorMessage("VOTE_DENIED", "Голосовать можно только после победы"));
            return;
        }

        if (idx < -1 || idx >= levels.length) {
            handler.send(new ErrorMessage("BAD_VOTE", "Некорректный выбор уровня"));
            return;
        }

        if (handler == fishClient) {
            fishVote = idx;
            System.out.println("Fish voted for " + (idx == -1 ? "AUTO" : ("level " + idx)));
        } else if (handler == crabClient) {
            crabVote = idx;
            System.out.println("Crab voted for " + (idx == -1 ? "AUTO" : ("level " + idx)));
        } else {
            return;
        }

        if (fishVote != null && crabVote != null) {
            if (fishVote.equals(crabVote)) {
                int chosen = fishVote;

                int targetLevel;
                if (chosen == -1) {
                    targetLevel = (currentLevelIndex + 1) % levels.length;
                } else {
                    targetLevel = chosen;
                }

                currentLevelIndex = targetLevel;
                this.gameState = loadLevel(currentLevelIndex);
                recreatePlayersAfterLevelChange();
                placePlayersForCurrentLevel();
                clearVotes();
                broadcastLevelDataToAll();
                broadcastState();
                System.out.println("VOTE AGREED: start level " + currentLevelIndex);
            } else {
                broadcast(new ErrorMessage(
                        "VOTE_FAIL",
                        "Оба игрока должны выбрать один и тот же вариант (Auto или один и тот же уровень)"
                ));
                clearVotes();
                System.out.println("VOTE CONFLICT: fish=" + fishVote + ", crab=" + crabVote);
            }
        }
    }

    private void clearVotes() {
        fishVote = null;
        crabVote = null;
    }

    private void handlePostWinAction(Player player) {
        if (player.getRole() == PlayerRole.FISH) {
            currentLevelIndex = (currentLevelIndex + 1) % levels.length;
            this.gameState = loadLevel(currentLevelIndex);
            recreatePlayersAfterLevelChange();
            placePlayersForCurrentLevel();
            clearVotes();
            broadcastLevelDataToAll();
            broadcastState();
            System.out.println("Next level (fallback): " + currentLevelIndex);
        } else if (player.getRole() == PlayerRole.CRAB) {
            this.gameState = loadLevel(currentLevelIndex);
            recreatePlayersAfterLevelChange();
            placePlayersForCurrentLevel();
            clearVotes();
            broadcastLevelDataToAll();
            broadcastState();
            System.out.println("Restart level (fallback): " + currentLevelIndex);
        }
    }

    private boolean isWalkable(int x, int y) {
        TileType tile = gameState.getMap().getTile(x, y);
        return tile != TileType.WALL;
    }

    private boolean isObjectBlocking(int x, int y) {
        for (GameObject obj : gameState.getObjects()) {
            if (obj.getPosition().getX() == x && obj.getPosition().getY() == y) {
                if (obj.getType() == ObjectType.BOX || obj.getType() == ObjectType.ROCK) {
                    return true;
                }
            }
        }
        return false;
    }

    private GameObject findObjectAt(int x, int y, ObjectType type) {
        for (GameObject obj : gameState.getObjects()) {
            if (obj.getType() == type &&
                    obj.getPosition().getX() == x &&
                    obj.getPosition().getY() == y) {
                return obj;
            }
        }
        return null;
    }

    private void activateMushroomAt(int x, int y) {
        for (GameObject obj : gameState.getObjects()) {
            if (obj.getType() == ObjectType.MUSHROOM &&
                    obj.getPosition().getX() == x &&
                    obj.getPosition().getY() == y) {
                obj.setActive(true);
            }
        }
    }

    private void checkLevelCompleted() {
        if (gameState.isLevelCompleted()) {
            return;
        }

        Player fish = gameState.getFish();
        Player crab = gameState.getCrab();
        if (fish == null || crab == null) return;

        Position pf = fish.getPosition();
        Position pc = crab.getPosition();

        TileType tf = gameState.getMap().getTile(pf.getX(), pf.getY());
        TileType tc = gameState.getMap().getTile(pc.getX(), pc.getY());

        if (tf == TileType.EXIT && tc == TileType.EXIT) {
            gameState.setLevelCompleted(true);
            clearVotes();
            System.out.println("LEVEL COMPLETED!");
        }
    }

    private Player findPlayerById(String clientId) {
        if (gameState.getFish() != null && clientId.equals(gameState.getFish().getId())) {
            return gameState.getFish();
        }
        if (gameState.getCrab() != null && clientId.equals(gameState.getCrab().getId())) {
            return gameState.getCrab();
        }
        return null;
    }

    private void recreatePlayersAfterLevelChange() {
        if (fishClient != null) {
            String id = fishClient.getClientId();
            String name = "Fish";
            Player fish = new Player(id, name, PlayerRole.FISH, new Position(1, 1));
            gameState.setFish(fish);
        }
        if (crabClient != null) {
            String id = crabClient.getClientId();
            String name = "Crab";
            Player crab = new Player(id, name, PlayerRole.CRAB, new Position(2, 5));
            gameState.setCrab(crab);
        }
    }

    private void broadcastLevelDataToAll() {
        CaveMap map = gameState.getMap();
        int w = map.getWidth();
        int h = map.getHeight();
        String[] rows = levels[currentLevelIndex];

        LevelDataMessage msg = new LevelDataMessage(w, h, rows);

        List<ClientHandler> clients = server.getClients();
        synchronized (clients) {
            for (ClientHandler c : clients) {
                c.send(msg);
            }
        }
    }

    private void broadcastState() {
        StringBuilder sb = new StringBuilder();

        if (gameState.getFish() != null) {
            Position pf = gameState.getFish().getPosition();
            sb.append("F:").append(pf.getX()).append(",").append(pf.getY());
        }
        sb.append(";");
        if (gameState.getCrab() != null) {
            Position pc = gameState.getCrab().getPosition();
            sb.append("C:").append(pc.getX()).append(",").append(pc.getY());
        }

        List<GameObject> objects = gameState.getObjects();
        if (!objects.isEmpty()) {
            sb.append(";O:");
            boolean first = true;
            for (GameObject obj : objects) {
                if (!first) {
                    sb.append("/");
                }
                first = false;
                char t = switch (obj.getType()) {
                    case MUSHROOM -> 'M';
                    case BOX -> 'B';
                    case ROCK -> 'R';
                };
                sb.append(t)
                        .append(",")
                        .append(obj.getPosition().getX())
                        .append(",")
                        .append(obj.getPosition().getY())
                        .append(",")
                        .append(obj.isActive() ? "1" : "0");
            }
        }

        if (gameState.isLevelCompleted()) {
            sb.append(";D:1");
        }

        StateUpdateMessage msg = new StateUpdateMessage(sb.toString());
        broadcast(msg);
    }

    private void broadcast(Message msg) {
        List<ClientHandler> clients = server.getClients();
        synchronized (clients) {
            for (ClientHandler c : clients) {
                c.send(msg);
            }
        }
    }
    private void restartCurrentLevelNoVote() {
        this.gameState = loadLevel(currentLevelIndex);
        recreatePlayersAfterLevelChange();
        placePlayersForCurrentLevel();
        clearVotes();

        restartRequested = false;
        restartRequester = null;

        broadcastLevelDataToAll();
        broadcastState();
        System.out.println("Level restarted by mutual agreement, level " + currentLevelIndex);
    }

    public synchronized void handleRestartRequest(ClientHandler handler, RestartRequestMessage msg) {
        if (fishClient == null && crabClient == null) {
            return;
        }

        if (fishClient == null || crabClient == null) {
            restartCurrentLevelNoVote();
            return;
        }

        if (restartRequested) {
            return;
        }

        restartRequested = true;
        restartRequester = handler;

        String fromName = "Partner";
        if (handler == fishClient && gameState.getFish() != null) {
            fromName = gameState.getFish().getName();
        } else if (handler == crabClient && gameState.getCrab() != null) {
            fromName = gameState.getCrab().getName();
        }

        ClientHandler other = (handler == fishClient) ? crabClient : fishClient;
        if (other != null) {
            other.send(new RestartOfferMessage(fromName));
        }
    }

    public synchronized void handleRestartResponse(ClientHandler handler, RestartResponseMessage msg) {
        if (!restartRequested || restartRequester == null) {
            return;
        }

        boolean accepted = msg.isAccepted();

        if (!accepted) {

            if (restartRequester != null) {
                restartRequester.send(new ErrorMessage(
                        "RESTART_DECLINED",
                        "Партнёр отклонил перезапуск уровня"
                ));
            }
            restartRequested = false;
            restartRequester = null;
            return;
        }

        restartCurrentLevelNoVote();
    }

    public synchronized void handleDisconnect(ClientHandler handler) {
        boolean isFish = (handler == fishClient);
        boolean isCrab = (handler == crabClient);

        if (!isFish && !isCrab) {
            return;
        }

        System.out.println((isFish ? "Fish" : "Crab") + " disconnected");

        fishClient = null;
        crabClient = null;

        if (gameState.getFish() != null) {
            gameState.setFish(null);
        }
        if (gameState.getCrab() != null) {
            gameState.setCrab(null);
        }

        clearVotes();

        gameState = loadLevel(currentLevelIndex);

        broadcast(new ErrorMessage(
                "PLAYER_LEFT",
                "Другой игрок отключился. Игра завершена, вернитесь в главное меню."
        ));

        restartRequested = false;
        restartRequester = null;
    }

}
