package symbiosis.client;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.control.ListCell;

import symbiosis.client.net.GameClient;
import symbiosis.client.ui.CompassView;
import symbiosis.client.ui.GameCanvas;
import symbiosis.client.ui.SkinTheme;
import symbiosis.client.ui.SoundManager;
import symbiosis.client.ui.ViewState;
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

    private String preferredRoleString = null;
    private int preferredLevelIndex = -1;

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

        root.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #020b1b, #041932, #030b20);"
        );

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
                event.consume();
            }
        });
    }

    private Pane createMenuPane() {
        StackPane rootPane = new StackPane();
        rootPane.setPadding(new Insets(20));

        VBox menuCard = new VBox(15);
        menuCard.setAlignment(Pos.CENTER);
        menuCard.setPadding(new Insets(20));
        menuCard.setMaxWidth(380);

        menuCard.setBackground(new Background(
                new BackgroundFill(
                        Color.rgb(10, 20, 40, 0.92),
                        new CornerRadii(18),
                        Insets.EMPTY
                )
        ));
        menuCard.setBorder(new Border(new BorderStroke(
                Color.rgb(80, 150, 255, 0.6),
                BorderStrokeStyle.SOLID,
                new CornerRadii(18),
                new BorderWidths(2)
        )));

        DropShadow shadow = new DropShadow();
        shadow.setRadius(20);
        shadow.setOffsetX(0);
        shadow.setOffsetY(8);
        shadow.setColor(Color.color(0.0, 0.4, 0.8, 0.65));
        menuCard.setEffect(shadow);

        Timeline glow = new Timeline(
                new KeyFrame(
                        Duration.ZERO,
                        new KeyValue(shadow.radiusProperty(), 16),
                        new KeyValue(shadow.colorProperty(), Color.color(0.1, 0.5, 1.0, 0.5))
                ),
                new KeyFrame(
                        Duration.seconds(2),
                        new KeyValue(shadow.radiusProperty(), 26),
                        new KeyValue(shadow.colorProperty(), Color.color(0.4, 0.8, 1.0, 0.9))
                )
        );
        glow.setAutoReverse(true);
        glow.setCycleCount(Timeline.INDEFINITE);
        glow.play();

        Label title = new Label("Symbiosis");
        title.setTextFill(Color.web("#7fd4ff"));
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        Label subtitle = new Label("co-op underwater puzzle");
        subtitle.setTextFill(Color.web("#9fb5ff"));
        subtitle.setStyle("-fx-font-size: 13px; -fx-opacity: 0.9;");

        TextField hostField = new TextField("localhost");
        TextField portField = new TextField("5555");
        TextField nameField = new TextField("Player");

        hostField.setMaxWidth(230);
        portField.setMaxWidth(230);
        nameField.setMaxWidth(230);

        String tfStyle =
                "-fx-background-radius: 10;" +
                        "-fx-background-color: rgba(9,16,35,0.95);" +
                        "-fx-text-fill: #e7f5ff;" +
                        "-fx-prompt-text-fill: #9abfff;" +
                        "-fx-border-color: #3b5a97;" +
                        "-fx-border-radius: 10;";
        hostField.setStyle(tfStyle);
        portField.setStyle(tfStyle);
        nameField.setStyle(tfStyle);

        ComboBox<SkinTheme> skinBox = new ComboBox<>();
        skinBox.getItems().addAll(SkinTheme.CLASSIC, SkinTheme.OCEAN, SkinTheme.DEEP);
        skinBox.setValue(SkinTheme.CLASSIC);
        skinBox.setMaxWidth(230);

        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("Auto", "Fish", "Crab");
        roleBox.setValue("Auto");
        roleBox.setMaxWidth(230);

        ComboBox<String> levelBox = new ComboBox<>();
        levelBox.getItems().addAll("Level 1", "Level 2");
        levelBox.setValue("Level 1");
        levelBox.setMaxWidth(230);

        String cbStyle =
                "-fx-background-radius: 10;" +
                        "-fx-background-color: rgba(9,16,35,0.95);" +
                        "-fx-text-fill: #e7f5ff;" +
                        "-fx-border-color: #3b5a97;" +
                        "-fx-border-radius: 10;";

        skinBox.setStyle(cbStyle);
        roleBox.setStyle(cbStyle);
        levelBox.setStyle(cbStyle);


        skinBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(SkinTheme item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.name());
                }
                setTextFill(Color.web("#10233d"));
            }
        });
        skinBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(SkinTheme item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.name());
                }
                setTextFill(Color.web("#e7f5ff"));
            }
        });

        roleBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    switch (item) {
                        case "Fish" -> setText("🐟 Fish");
                        case "Crab" -> setText("🦀 Crab");
                        default -> setText("🎲 Auto");
                    }
                }
                setTextFill(Color.web("#10233d"));
            }
        });

        roleBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    switch (item) {
                        case "Fish" -> setText("🐟 Fish");
                        case "Crab" -> setText("🦀 Crab");
                        default -> setText("🎲 Auto");
                    }
                }
                setTextFill(Color.web("#e7f5ff"));
            }
        });

        levelBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                }
                setTextFill(Color.web("#10233d"));
            }
        });

        levelBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                }
                setTextFill(Color.web("#e7f5ff"));
            }
        });


        Label skinLabel = new Label("Skin theme");
        Label roleLabel = new Label("Role");
        Label levelLabel = new Label("Start level");

        for (Label l : new Label[]{skinLabel, roleLabel, levelLabel}) {
            l.setTextFill(Color.web("#e9f3ff"));
            l.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        }

        Label roleHint = new Label("🐟 light & vision   |   🦀 strength & crates");
        roleHint.setTextFill(Color.web("#9fd6ff"));
        roleHint.setStyle("-fx-font-size: 11px; -fx-opacity: 0.9;");

        Button startButton = new Button("Start game");
        startButton.setPrefWidth(230);
        startButton.setStyle(
                "-fx-background-radius: 20;" +
                        "-fx-background-color: linear-gradient(to right, #29b6f6, #2962ff);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 8 18 8 18;"
        );
        startButton.setOnMouseEntered(e ->
                startButton.setStyle(
                        "-fx-background-radius: 20;" +
                                "-fx-background-color: linear-gradient(to right, #40c4ff, #3d73ff);" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-font-size: 14px;" +
                                "-fx-padding: 8 18 8 18;" +
                                "-fx-effect: dropshadow(gaussian, rgba(64,196,255,0.6), 12, 0.2, 0, 0);"
                )
        );
        startButton.setOnMouseExited(e ->
                startButton.setStyle(
                        "-fx-background-radius: 20;" +
                                "-fx-background-color: linear-gradient(to right, #29b6f6, #2962ff);" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-font-size: 14px;" +
                                "-fx-padding: 8 18 8 18;"
                )
        );

        Label statusLabel = new Label();
        statusLabel.setTextFill(Color.web("#ffd49e"));
        statusLabel.setStyle("-fx-font-size: 13px; -fx-effect: dropshadow(gaussian, rgba(255,212,158,0.6), 8,0.3,0,0);");

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

            String roleChoice = roleBox.getValue();
            if ("Fish".equalsIgnoreCase(roleChoice)) {
                preferredRoleString = "FISH";
            } else if ("Crab".equalsIgnoreCase(roleChoice)) {
                preferredRoleString = "CRAB";
            } else {
                preferredRoleString = null; // Auto
            }

            int levelIdx = levelBox.getSelectionModel().getSelectedIndex(); // 0 или 1
            preferredLevelIndex = levelIdx;

            statusLabel.setText("Connecting...");
            connectToServer(host, port, playerName, preferredRoleString, preferredLevelIndex, statusLabel);
        });

        VBox fields = new VBox(6,
                new Label("Host"), hostField,
                new Label("Port"), portField,
                new Label("Name"), nameField,
                skinLabel, skinBox,
                roleLabel, roleBox,
                roleHint,
                levelLabel, levelBox
        );

        for (int i = 0; i < fields.getChildren().size(); i++) {
            if (fields.getChildren().get(i) instanceof Label l &&
                    l != skinLabel && l != roleLabel && l != levelLabel) {

                l.setTextFill(Color.web("#d4e6ff"));
                l.setStyle("-fx-font-size: 12px; -fx-opacity: 1.0;");
            }
        }
        fields.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2, title, subtitle);
        titleBox.setAlignment(Pos.CENTER);

        menuCard.getChildren().addAll(
                titleBox,
                fields,
                startButton,
                statusLabel
        );

        rootPane.getChildren().add(menuCard);
        StackPane.setAlignment(menuCard, Pos.CENTER);

        return rootPane;
    }


    private BorderPane createGamePane() {
        roleLabel = new Label("Role: —");
        pauseLabel = new Label("");
        completedLabel = new Label("Completed: 0");
        connectionLabel = new Label("Not connected");

        roleLabel.setTextFill(Color.web("#e7f5ff"));
        pauseLabel.setTextFill(Color.web("#ffcc80"));
        completedLabel.setTextFill(Color.web("#a5ffb3"));
        connectionLabel.setTextFill(Color.web("#b0c7ff"));

        HBox topBar = new HBox(15,
                connectionLabel,
                new Separator(),
                roleLabel,
                pauseLabel,
                completedLabel
        );
        topBar.setPadding(new Insets(10));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setBackground(new Background(
                new BackgroundFill(
                        Color.rgb(6, 10, 22, 0.92),
                        CornerRadii.EMPTY,
                        Insets.EMPTY
                )
        ));

        gameCanvas = new GameCanvas(800, 600, viewState);
        compassView = new CompassView(120, viewState);

        StackPane compassWrapper = new StackPane(compassView);
        compassWrapper.setPadding(new Insets(6));
        compassWrapper.setBackground(new Background(
                new BackgroundFill(
                        Color.rgb(5, 12, 28, 0.95),
                        new CornerRadii(12),
                        Insets.EMPTY
                )
        ));
        compassWrapper.setBorder(new Border(new BorderStroke(
                Color.rgb(90, 160, 255, 0.8),
                BorderStrokeStyle.SOLID,
                new CornerRadii(12),
                new BorderWidths(1.5)
        )));

        Label compassTitle = new Label("Partner compass");
        compassTitle.setTextFill(Color.web("#e9f3ff"));
        compassTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        Label hudTitle = new Label("Controls");
        Label hudMove = new Label("WASD / ↑↓←→ – движение");
        Label hudAction = new Label("Space – действие");
        Label hudRestart = new Label("R – рестарт уровня (после победы, краб)");
        Label hudNext = new Label("N – следующий уровень (после победы, рыбка)");
        Label hudGoal = new Label("Цель: оба игрока должны стоять на выходе");
        Label hudFish = new Label("🐟 Рыбка: свет и активация грибов");
        Label hudCrab = new Label("🦀 Краб: толкает ящики, видит только в свету");
        Label hudPause = new Label("ESC – пауза");

        hudTitle.setTextFill(Color.web("#e9f3ff"));
        hudTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        for (Label l : new Label[]{hudMove, hudAction, hudRestart, hudNext, hudGoal, hudFish, hudCrab, hudPause}) {
            l.setTextFill(Color.web("#d4e6ff"));
            l.setStyle("-fx-font-size: 12px;");
        }

        VBox hudBox = new VBox(6,
                compassTitle,
                compassWrapper,
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
        hudBox.setPadding(new Insets(10));
        hudBox.setFillWidth(true);
        hudBox.setBackground(new Background(
                new BackgroundFill(
                        Color.rgb(8, 16, 35, 0.94),
                        new CornerRadii(14),
                        Insets.EMPTY
                )
        ));
        hudBox.setBorder(new Border(new BorderStroke(
                Color.rgb(70, 120, 200, 0.7),
                BorderStrokeStyle.SOLID,
                new CornerRadii(14),
                new BorderWidths(1.5)
        )));

        HBox centerRow = new HBox(10);
        centerRow.setPadding(new Insets(8));
        centerRow.setAlignment(Pos.CENTER_LEFT);

        StackPane canvasWrapper = new StackPane(gameCanvas);
        canvasWrapper.setPrefSize(800, 600);
        canvasWrapper.setBackground(new Background(
                new BackgroundFill(
                        Color.rgb(3, 8, 20, 0.9),
                        new CornerRadii(16),
                        Insets.EMPTY
                )
        ));
        canvasWrapper.setBorder(new Border(new BorderStroke(
                Color.rgb(40, 80, 150, 0.8),
                BorderStrokeStyle.SOLID,
                new CornerRadii(16),
                new BorderWidths(1.5)
        )));

        centerRow.getChildren().addAll(canvasWrapper, hudBox);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(6);
        logArea.setStyle(
                "-fx-control-inner-background: rgba(4,10,24,0.96);" +
                        "-fx-text-fill: #e7f5ff;" +
                        "-fx-highlight-fill: #2962ff;" +
                        "-fx-highlight-text-fill: white;" +
                        "-fx-border-color: #273754;"
        );

        chatInput = new TextField();
        chatInput.setStyle(
                "-fx-background-radius: 10;" +
                        "-fx-background-color: rgba(9,16,35,0.95);" +
                        "-fx-text-fill: #e7f5ff;" +
                        "-fx-prompt-text-fill: #9abfff;" +
                        "-fx-border-color: #3b5a97;" +
                        "-fx-border-radius: 10;"
        );

        sendChatButton = new Button("Send");
        sendChatButton.setStyle(
                "-fx-background-radius: 14;" +
                        "-fx-background-color: linear-gradient(to right, #29b6f6, #2962ff);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 12px;" +
                        "-fx-padding: 4 12 4 12;"
        );

        HBox chatBox = new HBox(5, chatInput, sendChatButton);
        chatBox.setAlignment(Pos.CENTER_LEFT);

        Label logLabel = new Label("Log / Chat:");
        logLabel.setTextFill(Color.web("#e9f3ff"));
        logLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        VBox bottomBox = new VBox(5, logLabel, logArea, chatBox);
        bottomBox.setPadding(new Insets(10));
        bottomBox.setBackground(new Background(
                new BackgroundFill(
                        Color.rgb(5, 10, 24, 0.94),
                        new CornerRadii(14),
                        Insets.EMPTY
                )
        ));

        sendChatButton.setOnAction(e -> sendChat());
        chatInput.setOnAction(e -> sendChat());

        BorderPane gameRoot = new BorderPane();
        gameRoot.setTop(topBar);
        gameRoot.setCenter(centerRow);
        gameRoot.setBottom(bottomBox);

        return gameRoot;
    }

    private void connectToServer(String host,
                                 int port,
                                 String playerName,
                                 String preferredRoleString,
                                 int preferredLevelIndex,
                                 Label statusLabel) {
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

                gameClient.send(new JoinMessage(playerName, preferredRoleString, preferredLevelIndex));

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

            String roleIcon = switch (role) {
                case FISH -> "🐟 ";
                case CRAB -> "🦀 ";
                default -> "";
            };
            connectionLabel.setText("Connected to " + host + ":" + port + " as " + playerName + " (" + roleIcon + role + ")");
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
        String icon = switch (role) {
            case FISH -> "🐟 ";
            case CRAB -> "🦀 ";
            default -> "";
        };
        roleLabel.setText("Role: " + icon + role);
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
