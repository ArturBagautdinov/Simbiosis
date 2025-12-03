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

        this.levels = new String[][]{ level0, level1 };

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
            if (currentLevelIndex == 0) {
                gameState.getFish().setPosition(new Position(1, 1));
            } else {
                gameState.getFish().setPosition(new Position(1, 1));
            }
        }
        if (gameState.getCrab() != null) {
            if (currentLevelIndex == 0) {
                gameState.getCrab().setPosition(new Position(2, 5));
            } else {
                gameState.getCrab().setPosition(new Position(10, 5));
            }
        }
    }

    public synchronized void handleJoin(ClientHandler handler, JoinMessage msg) {
        PlayerRole role;
        if (fishClient == null) {
            fishClient = handler;
            role = PlayerRole.FISH;
            Player fish = new Player(handler.getClientId(), msg.getPlayerName(),
                    role, new Position(1, 1));
            gameState.setFish(fish);
        } else if (crabClient == null) {
            crabClient = handler;
            role = PlayerRole.CRAB;
            Player crab = new Player(handler.getClientId(), msg.getPlayerName(),
                    role, new Position(2, 5));
            gameState.setCrab(crab);
        } else {
            handler.send(new ErrorMessage("FULL", "Server already has two players"));
            return;
        }

        placePlayersForCurrentLevel();

        handler.send(new RoleAssignedMessage(handler.getClientId(), role.name()));

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
            // Краб пытается толкнуть ящик
            int boxTargetX = targetX + dx;
            int boxTargetY = targetY + dy;

            if (isWalkable(boxTargetX, boxTargetY)
                    && !isObjectBlocking(boxTargetX, boxTargetY)) {
                // двигаем ящик и краба
                boxAtTarget.setPosition(new Position(boxTargetX, boxTargetY));
                p.setPosition(new Position(targetX, targetY));
            }
        } else {
            // Обычное движение (если нет ящика или это не краб)
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

    private void handlePostWinAction(Player player) {
        if (player.getRole() == PlayerRole.FISH) {
            currentLevelIndex = (currentLevelIndex + 1) % levels.length;
            this.gameState = loadLevel(currentLevelIndex);
            recreatePlayersAfterLevelChange();
            placePlayersForCurrentLevel();
            broadcastLevelDataToAll();
            broadcastState();
            System.out.println("Next level: " + currentLevelIndex);
        } else if (player.getRole() == PlayerRole.CRAB) {
            this.gameState = loadLevel(currentLevelIndex);
            recreatePlayersAfterLevelChange();
            placePlayersForCurrentLevel();
            broadcastLevelDataToAll();
            broadcastState();
            System.out.println("Restart level: " + currentLevelIndex);
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
}
