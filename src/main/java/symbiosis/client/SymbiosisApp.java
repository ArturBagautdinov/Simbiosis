package symbiosis.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import symbiosis.client.net.GameClient;
import symbiosis.client.ui.CompassView;
import symbiosis.client.ui.GameCanvas;
import symbiosis.client.ui.ViewState;
import symbiosis.client.ui.SkinTheme;
import symbiosis.client.ui.SoundManager;
import symbiosis.common.model.*;
import symbiosis.common.net.*;

import java.io.IOException;
import java.util.List;

public class SymbiosisApp extends Application {

    private GameClient client;
    private final ViewState viewState = new ViewState();
    private GameCanvas gameCanvas;
    private CompassView compassView;

    private TextArea logArea;
    private TextField chatInput;
    private Button sendChatButton;

    private boolean levelCompletedShown = false;
    private boolean paused = false;
    private int completedCount = 0;

    private Label roleLabel;
    private Label pauseLabel;
    private Label completedLabel;
    private Label connectionLabel;

    private StackPane root;
    private Pane menuPane;
    private BorderPane gamePane;

    private String host;
    private int port;
    private String playerName;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Symbiosis");

        gamePane = createGamePane();
        menuPane = createMenuPane();

        root = new StackPane(gamePane, menuPane);
        gamePane.setVisible(false);
        menuPane.setVisible(true);

        Scene scene = new Scene(root, 980, 750);
        primaryStage.setScene(scene);
        primaryStage.show();

        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (client == null) return;
            if (viewState.getClientId() == null) return;

            if (event.getCode() == KeyCode.ESCAPE) {
                paused = !paused;
                pauseLabel.setText(paused ? "PAUSED" : "");
                event.consume();
                return;
            }

            if (viewState.isLevelCompleted()) {
                PlayerRole role = viewState.getLocalRole();
                if (event.getCode() == KeyCode.R && role == PlayerRole.CRAB) {
                    client.send(new InputMessage(viewState.getClientId(), InputMessage.InputType.ACTION));
                } else if (event.getCode() == KeyCode.N && role == PlayerRole.FISH) {
                    client.send(new InputMessage(viewState.getClientId(), InputMessage.InputType.ACTION));
                }
                event.consume();
                return;
            }

            if (paused) return;

            InputMessage.InputType type = null;
            if (event.getCode() == KeyCode.W || event.getCode() == KeyCode.UP) {
                type = InputMessage.InputType.MOVE_UP;
            } else if (event.getCode() == KeyCode.S || event.getCode() == KeyCode.DOWN) {
                type = InputMessage.InputType.MOVE_DOWN;
            } else if (event.getCode() == KeyCode.A || event.getCode() == KeyCode.LEFT) {
                type = InputMessage.InputType.MOVE_LEFT;
            } else if (event.getCode() == KeyCode.D || event.getCode() == KeyCode.RIGHT) {
                type = InputMessage.InputType.MOVE_RIGHT;
            } else if (event.getCode() == KeyCode.SPACE) {
                type = InputMessage.InputType.ACTION;
            }

            if (type != null) {
                sendInput(type);
                event.consume(); // чтобы WASD не печатались в чат и поля
            }
        });

    }

    private Pane createMenuPane() {
        VBox menu = new VBox(10);
        menu.setPadding(new Insets(20));
        menu.setAlignment(Pos.CENTER);

        Label title = new Label("Symbiosis");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold;");

        TextField hostField = new TextField("localhost");
        TextField portField = new TextField("5555");
        TextField nameField = new TextField("Player");

        hostField.setMaxWidth(200);
        portField.setMaxWidth(200);
        nameField.setMaxWidth(200);

        ComboBox<SkinTheme> skinBox = new ComboBox<>();
        skinBox.getItems().addAll(SkinTheme.CLASSIC, SkinTheme.OCEAN, SkinTheme.DEEP);
        skinBox.setValue(SkinTheme.CLASSIC);
        skinBox.setMaxWidth(200);

        Label skinLabel = new Label("Skin theme:");

        Button startButton = new Button("Start Game");
        Label statusLabel = new Label();
        statusLabel.setTextFill(Color.ORANGE);

        startButton.setOnAction(e -> {
            host = hostField.getText().trim();
            if (host.isEmpty()) host = "localhost";
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException ex) {
                statusLabel.setText("Invalid port");
                return;
            }
            playerName = nameField.getText().trim();
            if (playerName.isEmpty()) {
                statusLabel.setText("Enter name");
                return;
            }

            viewState.setSkinTheme(skinBox.getValue());
            statusLabel.setText("Connecting...");
            connectToServer(host, port, playerName, statusLabel);
        });

        VBox fields = new VBox(5,
                new Label("Host:"), hostField,
                new Label("Port:"), portField,
                new Label("Name:"), nameField,
                skinLabel, skinBox
        );
        fields.setAlignment(Pos.CENTER_LEFT);

        menu.getChildren().addAll(title, fields, startButton, statusLabel);
        return menu;
    }

    private BorderPane createGamePane() {
        roleLabel = new Label("Role: —");
        roleLabel.setTextFill(Color.LIGHTGRAY);

        pauseLabel = new Label("");
        pauseLabel.setTextFill(Color.ORANGE);

        completedLabel = new Label("Completed: 0");
        completedLabel.setTextFill(Color.LIGHTGREEN);

        connectionLabel = new Label("Not connected");
        connectionLabel.setTextFill(Color.LIGHTGRAY);

        HBox topBar = new HBox(15,
                connectionLabel,
                new Separator(),
                roleLabel,
                pauseLabel,
                completedLabel
        );
        topBar.setPadding(new Insets(10));
        topBar.setAlignment(Pos.CENTER_LEFT);

        gameCanvas = new GameCanvas(800, 600, viewState);
        compassView = new CompassView(120, viewState);

        Label hudTitle = new Label("Controls:");
        Label hudMove = new Label("WASD / ↑↓←→ – движение");
        Label hudAction = new Label("Space – действие");
        Label hudRestart = new Label("R – рестарт уровня (после победы, краб)");
        Label hudNext = new Label("N – следующий уровень (после победы, рыбка)");
        Label hudGoal = new Label("Цель: оба игрока должны стоять на выходе");
        Label hudFish = new Label("Рыбка: свет и активация грибов");
        Label hudCrab = new Label("Краб: толкает ящики, видит только в свету");
        Label hudPause = new Label("ESC – пауза");

        VBox hudBox = new VBox(4,
                new Label("Partner compass:"),
                compassView,
                new Separator(),
                hudTitle,
                hudMove,
                hudAction,
                hudPause,
                hudRestart,
                hudNext,
                hudGoal,
                new Separator(),
                hudFish,
                hudCrab
        );
        hudBox.setAlignment(Pos.TOP_LEFT);
        hudBox.setPadding(new Insets(5));
        hudBox.setFillWidth(true);

        HBox centerRow = new HBox(10);
        centerRow.setPadding(new Insets(5));
        centerRow.setAlignment(Pos.CENTER_LEFT);

        StackPane canvasWrapper = new StackPane(gameCanvas);
        canvasWrapper.setPrefSize(800, 600);

        centerRow.getChildren().addAll(canvasWrapper, hudBox);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(6);

        chatInput = new TextField();
        sendChatButton = new Button("Send");
        HBox chatBox = new HBox(5, chatInput, sendChatButton);
        chatBox.setAlignment(Pos.CENTER_LEFT);

        VBox bottomBox = new VBox(5, new Label("Log / Chat:"), logArea, chatBox);
        bottomBox.setPadding(new Insets(10));

        sendChatButton.setOnAction(e -> sendChat());
        chatInput.setOnAction(e -> sendChat());

        BorderPane gameRoot = new BorderPane();
        gameRoot.setTop(topBar);
        gameRoot.setCenter(centerRow);
        gameRoot.setBottom(bottomBox);

        return gameRoot;
    }

    private void connectToServer(String host, int port, String playerName, Label statusLabel) {
        if (client != null) {
            statusLabel.setText("Already connected (or connecting)");
            return;
        }

        appendLog("Connecting to " + host + ":" + port + " ...");

        new Thread(() -> {
            try {
                GameClient gameClient = new GameClient(host, port);
                gameClient.setOnMessage(msg ->
                        Platform.runLater(() -> handleServerMessage(msg))
                );
                gameClient.connect();
                gameClient.send(new JoinMessage(playerName));

                Platform.runLater(() -> {
                    this.client = gameClient;
                    statusLabel.setText("Connected. Waiting for role...");
                    appendLog("Connected, JOIN sent as " + playerName);
                    connectionLabel.setText("Connected to " + host + ":" + port + " as " + playerName);
                });

            } catch (IOException ex) {
                Platform.runLater(() -> {
                    statusLabel.setText("Connection failed: " + ex.getMessage());
                    appendLog("Connection failed: " + ex.getMessage());
                });
            }
        }, "ConnectThread").start();
    }

    private void handleServerMessage(Message msg) {
        if (msg instanceof RoleAssignedMessage roleMsg) {
            viewState.setClientId(roleMsg.getPlayerId());
            PlayerRole role = PlayerRole.valueOf(roleMsg.getRole());
            viewState.setLocalRole(role);
            updateRoleLabel(role);
            appendLog("Role assigned: " + role + ", id=" + roleMsg.getPlayerId());

            if (menuPane != null) {
                menuPane.setVisible(false);
            }
            if (gamePane != null) {
                gamePane.setVisible(true);
            }

            connectionLabel.setText("Connected to " + host + ":" + port + " as " + playerName + " (" + role + ")");
        } else if (msg instanceof LevelDataMessage levelMsg) {
            applyLevelData(levelMsg);
        } else if (msg instanceof StateUpdateMessage stateMsg) {
            boolean wasCompleted = viewState.isLevelCompleted();
            parseAndApplyState(stateMsg.getPayload());

            if (viewState.isLevelCompleted() && !wasCompleted) {
                completedCount++;
                completedLabel.setText("Completed: " + completedCount);
                if (!levelCompletedShown) {
                    levelCompletedShown = true;
                    showLevelCompletedDialog();
                }
            } else if (!viewState.isLevelCompleted()) {
                levelCompletedShown = false;
            }
        } else if (msg instanceof ChatMessage chatMsg) {
            appendLog(chatMsg.getFrom() + ": " + chatMsg.getText());
        } else if (msg instanceof ErrorMessage err) {
            appendLog("ERROR " + err.getErrorCode() + ": " + err.getErrorText());
        } else {
            appendLog("Received: " + msg.getClass().getSimpleName());
        }
    }

    private void updateRoleLabel(PlayerRole role) {
        roleLabel.setText("Role: " + role);
        if (role == PlayerRole.FISH) {
            roleLabel.setTextFill(Color.CORNFLOWERBLUE);
        } else if (role == PlayerRole.CRAB) {
            roleLabel.setTextFill(Color.CRIMSON);
        } else {
            roleLabel.setTextFill(Color.LIGHTGRAY);
        }
    }

    private void applyLevelData(LevelDataMessage msg) {
        int w = msg.getWidth();
        int h = msg.getHeight();
        String[] rows = msg.getRows();

        TileType[][] tiles = new TileType[h][w];

        for (int y = 0; y < h; y++) {
            String row = rows[y];
            for (int x = 0; x < w; x++) {
                char c = row.charAt(x);
                tiles[y][x] = charToTile(c);
            }
        }

        viewState.setMap(tiles, w, h);
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

    private void parseAndApplyState(String payload) {
        if (payload == null) return;

        List<GameObject> objects = new java.util.ArrayList<>();

        String[] parts = payload.split(";");
        boolean levelCompleted = false;

        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            String[] kv = part.split(":", 2);
            if (kv.length != 2) continue;

            String key = kv[0];
            String value = kv[1];

            if ("F".equalsIgnoreCase(key) || "FISH".equalsIgnoreCase(key)) {
                String[] xy = value.split(",");
                if (xy.length == 2) {
                    try {
                        int x = Integer.parseInt(xy[0]);
                        int y = Integer.parseInt(xy[1]);
                        viewState.setFishPosition(new Position(x, y));
                    } catch (NumberFormatException ignored) {
                    }
                }
            } else if ("C".equalsIgnoreCase(key) || "CRAB".equalsIgnoreCase(key)) {
                String[] xy = value.split(",");
                if (xy.length == 2) {
                    try {
                        int x = Integer.parseInt(xy[0]);
                        int y = Integer.parseInt(xy[1]);
                        viewState.setCrabPosition(new Position(x, y));
                    } catch (NumberFormatException ignored) {
                    }
                }
            } else if ("O".equalsIgnoreCase(key)) {
                String[] entries = value.split("/");
                for (String e : entries) {
                    e = e.trim();
                    if (e.isEmpty()) continue;
                    String[] fields = e.split(",");
                    if (fields.length < 4) continue;
                    char typeChar = fields[0].charAt(0);
                    try {
                        int x = Integer.parseInt(fields[1]);
                        int y = Integer.parseInt(fields[2]);
                        boolean active = "1".equals(fields[3]);

                        ObjectType type = switch (typeChar) {
                            case 'M' -> ObjectType.MUSHROOM;
                            case 'B' -> ObjectType.BOX;
                            case 'R' -> ObjectType.ROCK;
                            default -> null;
                        };
                        if (type != null) {
                            GameObject obj = new GameObject(type, new Position(x, y));
                            obj.setActive(active);
                            objects.add(obj);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            } else if ("D".equalsIgnoreCase(key)) {
                if ("1".equals(value.trim())) {
                    levelCompleted = true;
                }
            }
        }

        viewState.setObjects(objects);
        viewState.setLevelCompleted(levelCompleted);
    }

    private void sendInput(InputMessage.InputType type) {
        if (client == null) return;
        if (viewState.isLevelCompleted()) return;
        if (paused) return;
        String clientId = viewState.getClientId();
        if (clientId == null) return;

        client.send(new InputMessage(clientId, type));

        if (type == InputMessage.InputType.MOVE_UP
                || type == InputMessage.InputType.MOVE_DOWN
                || type == InputMessage.InputType.MOVE_LEFT
                || type == InputMessage.InputType.MOVE_RIGHT) {
            SoundManager.playMove();
        } else if (type == InputMessage.InputType.ACTION) {
            SoundManager.playAction();
        }
    }

    private void sendChat() {
        if (client == null) {
            appendLog("Not connected");
            return;
        }
        String text = chatInput.getText().trim();
        if (text.isEmpty()) return;
        client.send(new ChatMessage(playerName, text));
        chatInput.clear();
    }

    private void appendLog(String text) {
        if (logArea == null) return;
        if (logArea.getText().isEmpty()) {
            logArea.setText(text);
        } else {
            logArea.appendText("\n" + text);
        }
    }

    private void showLevelCompletedDialog() {
        appendLog("LEVEL COMPLETED!");

        SoundManager.playWin();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Symbiosis");
        alert.setHeaderText("Level completed!");
        alert.setContentText("Оба игрока достигли выхода. Отличная работа!");
        alert.show();
    }

    @Override
    public void stop() {
        if (client != null) {
            client.disconnect();
        }
        if (compassView != null) {
            compassView.stop();
        }
        if (gameCanvas != null) {
            gameCanvas.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
