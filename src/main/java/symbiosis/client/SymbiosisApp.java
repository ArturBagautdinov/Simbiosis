package symbiosis.client;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
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
import java.util.Collections;
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

    private VBox menuCardRef;
    private HBox topBarRef;
    private VBox hudBoxRef;
    private StackPane canvasWrapperRef;
    private VBox bottomBoxRef;
    private Label menuStatusLabel;

    private static final String[][] LEVEL_PREVIEWS = new String[][]{
            {
                    "############",
                    "#..........#",
                    "#..D....L..#",
                    "#..####....#",
                    "#..#..M....#",
                    "#..#..B....#",
                    "#......E...#",
                    "############"
            },
            {
                    "############",
                    "#..M....B..#",
                    "#..####....#",
                    "#..D.......#",
                    "#......L...#",
                    "#..B....M..#",
                    "#...E......#",
                    "############"
            },
            {
                    "############",
                    "#..D....M..#",
                    "#..####....#",
                    "#..B....L..#",
                    "#..#..B....#",
                    "#..#....M..#",
                    "#...E......#",
                    "############"
            },
            {
                    "############",
                    "#..M....B..#",
                    "###.####...#",
                    "#..D....L..#",
                    "#..B..M....#",
                    "#..####....#",
                    "#E.......B.#",
                    "############"
            },
            {
                    "############",
                    "#M...B..D..#",
                    "#.####.###.#",
                    "#...L..M...#",
                    "#.B..###...#",
                    "#...B...M..#",
                    "#..E.......#",
                    "############"
            },
            {
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
            },
            {
                    "##################",
                    "#..M..###....L..E#",
                    "#.##..#..B..###..#",
                    "#..B..#..#..M..#.#",
                    "#..##.####.##..#.#",
                    "#..M..B..D..B..#.#",
                    "#..L..#..M..#..#.#",
                    "#..#..####..#..#.#",
                    "#..#..B..B..#..#.#",
                    "#..####..####..#.#",
                    "#M.....L....M..#.#",
                    "#.####.##.####.#.#",
                    "#..B..D..B..L..B.#",
                    "#..M......M....E.#",
                    "##################"
            },
            {
                    "##################",
                    "#..M..###..B....E#",
                    "#.##B.#..######..#",
                    "#..#..#B..M..#..##",
                    "##.#..####.#..#..#",
                    "#..#..D..#.#B.#..#",
                    "#B.####.#.#.#.##B#",
                    "#..#M..#.#.#..#..#",
                    "#..##..#.#.##.#B.#",
                    "#..#..#.#B..#....#",
                    "#.M#B.#.####.#.###",
                    "#..#.......M.#...#",
                    "#B.####.##.###.#.#",
                    "#..M..B..L.....#.#",
                    "##################"
            }
    };

    private static final String[] LEVEL_DIFFICULTY = new String[]{
            "★☆☆☆☆",
            "★★☆☆☆",
            "★★★☆☆",
            "★★★★☆",
            "★★★★☆",
            "★★★★☆",
            "★★★★★",
            "★★★★★"
    };


    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Symbiosis");
        viewState.setSkinTheme(SkinTheme.CLASSIC);

        gamePane = createGamePane();
        menuPane = createMenuPane();

        root = new StackPane(gamePane, menuPane);
        gamePane.setVisible(false);
        menuPane.setVisible(true);

        Scene scene = new Scene(root, 980, 750);
        primaryStage.setScene(scene);
        primaryStage.show();

        applyThemeToUi();

        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (client == null) return;
            if (viewState.getClientId() == null) return;

            if (event.getCode() == KeyCode.ESCAPE) {
                if (!paused) {
                    paused = true;
                    pauseLabel.setText("PAUSED");

                    Dialog<ButtonType> dialog = new Dialog<>();
                    dialog.setTitle("Pause");
                    dialog.setHeaderText("Game paused");

                    ButtonType resumeBtn = new ButtonType("Continue", ButtonBar.ButtonData.OK_DONE);
                    ButtonType menuBtn = new ButtonType("Main Menu", ButtonBar.ButtonData.LEFT);
                    ButtonType cancelBtn = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);

                    dialog.getDialogPane().getButtonTypes().addAll(resumeBtn, menuBtn, cancelBtn);

                    dialog.showAndWait().ifPresent(type -> {
                        if (type == menuBtn) {
                            returnToMainMenu();
                        } else if (type == resumeBtn) {
                            paused = false;
                            pauseLabel.setText("");
                        }
                    });

                } else {
                    paused = false;
                    pauseLabel.setText("");
                }

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

        menuCardRef = new VBox(15);
        menuCardRef.setAlignment(Pos.CENTER);
        menuCardRef.setPadding(new Insets(20));
        menuCardRef.setMaxWidth(420);

        menuCardRef.setBackground(new Background(
                new BackgroundFill(
                        Color.rgb(10, 20, 40, 0.92),
                        new CornerRadii(18),
                        Insets.EMPTY
                )
        ));
        menuCardRef.setBorder(new Border(new BorderStroke(
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
        menuCardRef.setEffect(shadow);

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
        skinBox.setValue(viewState.getSkinTheme());
        skinBox.setMaxWidth(230);

        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("Auto", "Fish", "Crab");
        roleBox.setValue("Auto");
        roleBox.setMaxWidth(230);

        ComboBox<String> levelBox = new ComboBox<>();
        levelBox.getItems().addAll(
                "Level 1",
                "Level 2",
                "Level 3",
                "Level 4",
                "Level 5",
                "Level 6",
                "Level 7",
                "Level 8"
        );
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

        skinBox.setButtonCell(new ListCell<SkinTheme>() {
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
            }
        });
        roleBox.setButtonCell(roleBox.getCellFactory().call(null));

        roleBox.setButtonCell(new ListCell<String>() {
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

        levelBox.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
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

        Label previewLabel = new Label("Level preview");
        previewLabel.setTextFill(Color.web("#e9f3ff"));
        previewLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        Label difficultyLabel = new Label("Difficulty: " + LEVEL_DIFFICULTY[0]);
        difficultyLabel.setTextFill(Color.web("#ffd49e"));
        difficultyLabel.setStyle("-fx-font-size: 12px;");

        GridPane levelPreview = createLevelPreviewPane();
        renderLevelPreview(levelPreview, 0);

        VBox previewLegend = new VBox(3,
                createLegendItem(Color.rgb(10, 20, 40), "Wall (#)"),
                createLegendItem(Color.rgb(25, 40, 70), "Empty (.)"),
                createLegendItem(Color.CYAN, "Exit (E)"),
                createLegendItem(Color.LIMEGREEN, "Mushroom (M)"),
                createLegendItem(Color.SANDYBROWN, "Box (B)"),
                createLegendItem(Color.rgb(5, 5, 20), "Dark (D)"),
                createLegendItem(Color.rgb(180, 220, 255), "Light (L)")
        );
        previewLegend.setAlignment(Pos.TOP_LEFT);
        previewLegend.setPadding(new Insets(4, 0, 4, 0));

        levelBox.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> {
            int idx = (newV == null) ? 0 : newV.intValue();
            if (idx < 0 || idx >= LEVEL_PREVIEWS.length) {
                idx = 0;
            }
            renderLevelPreview(levelPreview, idx);
            difficultyLabel.setText("Difficulty: " + LEVEL_DIFFICULTY[idx]);
        });

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

        menuStatusLabel = new Label();
        menuStatusLabel.setTextFill(Color.web("#ffd49e"));
        menuStatusLabel.setStyle("-fx-font-size: 13px; -fx-effect: dropshadow(gaussian, rgba(255,212,158,0.6), 8,0.3,0,0);");

        skinBox.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                viewState.setSkinTheme(newV);
                applyThemeToUi();
            }
        });

        startButton.setOnAction(e -> {
            host = hostField.getText().trim();
            if (host.isEmpty()) host = "localhost";
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException ex) {
                menuStatusLabel.setText("Invalid port");
                return;
            }
            playerName = nameField.getText().trim();
            if (playerName.isEmpty()) {
                menuStatusLabel.setText("Enter name");
                return;
            }

            viewState.setSkinTheme(skinBox.getValue());
            applyThemeToUi();

            String roleChoice = roleBox.getValue();
            if ("Fish".equalsIgnoreCase(roleChoice)) {
                preferredRoleString = "FISH";
            } else if ("Crab".equalsIgnoreCase(roleChoice)) {
                preferredRoleString = "CRAB";
            } else {
                preferredRoleString = null;
            }

            int levelIdx = levelBox.getSelectionModel().getSelectedIndex();
            preferredLevelIndex = levelIdx;

            menuStatusLabel.setText("Connecting...");
            connectToServer(host, port, playerName, preferredRoleString, preferredLevelIndex, menuStatusLabel);
        });

        VBox fieldsLeft = new VBox(6,
                new Label("Host"), hostField,
                new Label("Port"), portField,
                new Label("Name"), nameField,
                skinLabel, skinBox,
                roleLabel, roleBox,
                roleHint,
                levelLabel, levelBox
        );
        for (Node n : fieldsLeft.getChildren()) {
            if (n instanceof Label l &&
                    l != skinLabel && l != roleLabel && l != levelLabel && l != roleHint) {
                l.setTextFill(Color.web("#d4e6ff"));
                l.setStyle("-fx-font-size: 12px; -fx-opacity: 1.0;");
            }
        }
        fieldsLeft.setAlignment(Pos.CENTER_LEFT);

        VBox rightPreviewBox = new VBox(4, previewLabel, difficultyLabel, previewLegend, levelPreview);
        rightPreviewBox.setAlignment(Pos.TOP_CENTER);

        HBox middleRow = new HBox(15, fieldsLeft, rightPreviewBox);
        middleRow.setAlignment(Pos.CENTER);

        VBox titleBox = new VBox(2, title, subtitle);
        titleBox.setAlignment(Pos.CENTER);

        menuCardRef.getChildren().addAll(
                titleBox,
                middleRow,
                startButton,
                menuStatusLabel
        );

        rootPane.getChildren().add(menuCardRef);
        StackPane.setAlignment(menuCardRef, Pos.CENTER);

        return rootPane;
    }

    private GridPane createLevelPreviewPane() {
        GridPane grid = new GridPane();
        grid.setHgap(1);
        grid.setVgap(1);
        grid.setPadding(new Insets(4));
        grid.setBackground(new Background(
                new BackgroundFill(
                        Color.rgb(3, 8, 20, 0.9),
                        new CornerRadii(10),
                        Insets.EMPTY
                )
        ));
        grid.setBorder(new Border(new BorderStroke(
                Color.rgb(70, 120, 200, 0.8),
                BorderStrokeStyle.SOLID,
                new CornerRadii(10),
                new BorderWidths(1.5)
        )));
        return grid;
    }

    private void renderLevelPreview(GridPane grid, int levelIndex) {
        grid.getChildren().clear();
        if (levelIndex < 0 || levelIndex >= LEVEL_PREVIEWS.length) return;

        String[] rows = LEVEL_PREVIEWS[levelIndex];
        for (int y = 0; y < rows.length; y++) {
            String row = rows[y];
            for (int x = 0; x < row.length(); x++) {
                char c = row.charAt(x);
                Rectangle r = new Rectangle(10, 10);
                Color fill;
                switch (c) {
                    case '#':
                        fill = Color.rgb(10, 20, 40);
                        break;
                    case 'E':
                        fill = Color.CYAN;
                        break;
                    case 'M':
                        fill = Color.LIMEGREEN;
                        break;
                    case 'B':
                        fill = Color.SANDYBROWN;
                        break;
                    case 'D':
                        fill = Color.rgb(5, 5, 20);
                        break;
                    case 'L':
                        fill = Color.rgb(180, 220, 255);
                        break;
                    default:
                        fill = Color.rgb(25, 40, 70);
                        break;
                }
                r.setFill(fill);
                r.setStroke(Color.rgb(5, 10, 20));
                r.setStrokeWidth(0.5);
                GridPane.setRowIndex(r, y);
                GridPane.setColumnIndex(r, x);
                grid.getChildren().add(r);
            }
        }
    }

    private HBox createLegendItem(Color color, String text) {
        Rectangle rect = new Rectangle(12, 12, color);
        rect.setArcWidth(3);
        rect.setArcHeight(3);
        rect.setStroke(Color.rgb(5, 10, 20));
        rect.setStrokeWidth(0.5);

        Label label = new Label(text);
        label.setTextFill(Color.web("#e7f5ff"));
        label.setStyle("-fx-font-size: 11px;");

        HBox box = new HBox(6, rect, label);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
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

        topBarRef = new HBox(15,
                connectionLabel,
                new Separator(),
                roleLabel,
                pauseLabel,
                completedLabel
        );
        topBarRef.setPadding(new Insets(10));
        topBarRef.setAlignment(Pos.CENTER_LEFT);
        topBarRef.setBackground(new Background(
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

        Button exitToMenuButton = new Button("Main Menu");
        exitToMenuButton.setStyle(
                "-fx-background-radius: 14;" +
                        "-fx-background-color: linear-gradient(to right, #ff7043, #f4511e);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 12px;" +
                        "-fx-padding: 4 12 4 12;"
        );
        exitToMenuButton.setOnAction(e -> returnToMainMenu());

        Button restartButton = new Button("Restart level");
        restartButton.setStyle(
                "-fx-background-radius: 14;" +
                        "-fx-background-color: linear-gradient(to right, #ffa726, #fb8c00);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 12px;" +
                        "-fx-padding: 4 12 4 12;"
        );
        restartButton.setOnAction(e -> onRestartClicked());

        hudBoxRef = new VBox(6,
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
                hudCrab,
                new Separator(),
                restartButton,
                exitToMenuButton
        );
        hudBoxRef.setAlignment(Pos.TOP_LEFT);
        hudBoxRef.setPadding(new Insets(10));
        hudBoxRef.setFillWidth(true);
        hudBoxRef.setBackground(new Background(
                new BackgroundFill(
                        Color.rgb(8, 16, 35, 0.94),
                        new CornerRadii(14),
                        Insets.EMPTY
                )
        ));
        hudBoxRef.setBorder(new Border(new BorderStroke(
                Color.rgb(70, 120, 200, 0.7),
                BorderStrokeStyle.SOLID,
                new CornerRadii(14),
                new BorderWidths(1.5)
        )));

        HBox centerRow = new HBox(10);
        centerRow.setPadding(new Insets(8));
        centerRow.setAlignment(Pos.CENTER_LEFT);

        canvasWrapperRef = new StackPane(gameCanvas);
        canvasWrapperRef.setPrefSize(800, 600);
        canvasWrapperRef.setBackground(new Background(
                new BackgroundFill(
                        Color.rgb(3, 8, 20, 0.9),
                        new CornerRadii(16),
                        Insets.EMPTY
                )
        ));
        canvasWrapperRef.setBorder(new Border(new BorderStroke(
                Color.rgb(40, 80, 150, 0.8),
                BorderStrokeStyle.SOLID,
                new CornerRadii(16),
                new BorderWidths(1.5)
        )));

        centerRow.getChildren().addAll(canvasWrapperRef, hudBoxRef);

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

        bottomBoxRef = new VBox(5, logLabel, logArea, chatBox);
        bottomBoxRef.setPadding(new Insets(10));
        bottomBoxRef.setBackground(new Background(
                new BackgroundFill(
                        Color.rgb(5, 10, 24, 0.94),
                        new CornerRadii(14),
                        Insets.EMPTY
                )
        ));

        sendChatButton.setOnAction(e -> sendChat());
        chatInput.setOnAction(e -> sendChat());

        BorderPane gameRoot = new BorderPane();
        gameRoot.setTop(topBarRef);
        gameRoot.setCenter(centerRow);
        gameRoot.setBottom(bottomBoxRef);

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
            if ("VOTE_FAIL".equals(err.getErrorCode())) {
                Alert voteAlert = new Alert(Alert.AlertType.WARNING);
                voteAlert.setTitle("Vote mismatch");
                voteAlert.setHeaderText("Голосование не удалось");
                voteAlert.setContentText("Оба игрока должны выбрать один и тот же вариант (Auto или один и тот же уровень).");
                voteAlert.show();
            } else if ("PLAYER_LEFT".equals(err.getErrorCode())) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Player disconnected");
                alert.setHeaderText("Другой игрок отключился");
                alert.setContentText("Игра остановлена. Вы будете возвращены в главное меню.");
                alert.showAndWait();
                returnToMainMenu();
            }

            appendLog("ERROR " + err.getErrorCode() + ": " + err.getErrorText());

        } else if (msg instanceof RestartOfferMessage offer) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Restart request");
            alert.setHeaderText(offer.getFromName() + " хочет начать уровень заново");
            alert.setContentText("Перезапустить уровень?");

            ButtonType yesBtn = new ButtonType("Restart", ButtonBar.ButtonData.OK_DONE);
            ButtonType noBtn = new ButtonType("Continue", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(yesBtn, noBtn);

            alert.showAndWait().ifPresent(bt -> {
                boolean accepted = (bt == yesBtn);
                if (client != null && viewState.getClientId() != null) {
                    client.send(new RestartResponseMessage(viewState.getClientId(), accepted));
                }
                if (accepted) {
                    appendLog("You agreed to restart the level.");
                } else {
                    appendLog("You declined the restart request.");
                }
            });
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
                    e = trimSafe(e);
                    if (e.isEmpty()) continue;
                    String[] fields = e.split(",");
                    if (fields.length < 4) continue;
                    char typeChar = fields[0].charAt(0);
                    try {
                        int x = Integer.parseInt(fields[1]);
                        int y = Integer.parseInt(fields[2]);
                        boolean active = "1".equals(fields[3]);

                        ObjectType type = null;
                        switch (typeChar) {
                            case 'M' -> type = ObjectType.MUSHROOM;
                            case 'B' -> type = ObjectType.BOX;
                            case 'R' -> type = ObjectType.ROCK;
                        }
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

    private String trimSafe(String s) {
        return s == null ? "" : s.trim();
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

        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Symbiosis");
        dialog.setHeaderText("Level completed!\nВыберите уровень для голосования");

        ButtonType voteButtonType = new ButtonType("Vote", ButtonBar.ButtonData.OK_DONE);
        ButtonType skipButtonType = new ButtonType("Skip", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(voteButtonType, skipButtonType);

        ComboBox<String> levelBox = new ComboBox<>();
        levelBox.getItems().add("Auto (next level)");
        levelBox.getItems().addAll(
                "Level 1",
                "Level 2",
                "Level 3",
                "Level 4",
                "Level 5",
                "Level 6",
                "Level 7",
                "Level 8"
        );
        levelBox.getSelectionModel().select(0);

        VBox content = new VBox(8,
                new Label("Если оба игрока выберут один и тот же вариант (Auto или один и тот же уровень), он будет запущен."),
                new Label("Ваш выбор:"),
                levelBox
        );
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(button -> {
            if (button == voteButtonType) {
                int selected = levelBox.getSelectionModel().getSelectedIndex();
                if (selected == 0) {
                    return -1;
                } else {
                    return selected - 1;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(idx -> {
            if (idx != null && client != null && viewState.getClientId() != null) {
                client.send(new LevelVoteMessage(viewState.getClientId(), idx));
                if (idx == -1) {
                    appendLog("You voted: Auto (next level)");
                } else {
                    appendLog("You voted for Level " + (idx + 1));
                }
            }
        });
    }

    private void applyThemeToUi() {
        if (viewState.getSkinTheme() == null) return;
        SkinTheme theme = viewState.getSkinTheme();
        SkinTheme.ThemePalette p = theme.palette();

        if (root != null) {
            if (theme == SkinTheme.CLASSIC) {
                root.setStyle(
                        "-fx-background-color: linear-gradient(to bottom, #020b1b, #041932, #030b20);"
                );
            } else if (theme == SkinTheme.OCEAN) {
                root.setStyle(
                        "-fx-background-color: linear-gradient(to bottom, #00101c, #004066, #00101c);"
                );
            } else {
                root.setStyle(
                        "-fx-background-color: linear-gradient(to bottom, #05020d, #160637, #05020d);"
                );
            }
        }

        if (menuCardRef != null) {
            menuCardRef.setBackground(new Background(
                    new BackgroundFill(
                            p.menuCardBackground(),
                            new CornerRadii(18),
                            Insets.EMPTY
                    )
            ));
            menuCardRef.setBorder(new Border(new BorderStroke(
                    p.menuCardBorder(),
                    BorderStrokeStyle.SOLID,
                    new CornerRadii(18),
                    new BorderWidths(2)
            )));
        }

        if (topBarRef != null) {
            topBarRef.setBackground(new Background(
                    new BackgroundFill(
                            p.topBarBackground(),
                            CornerRadii.EMPTY,
                            Insets.EMPTY
                    )
            ));
        }

        if (hudBoxRef != null) {
            hudBoxRef.setBackground(new Background(
                    new BackgroundFill(
                            p.hudBackground(),
                            new CornerRadii(14),
                            Insets.EMPTY
                    )
            ));
            hudBoxRef.setBorder(new Border(new BorderStroke(
                    p.hudBorder(),
                    BorderStrokeStyle.SOLID,
                    new CornerRadii(14),
                    new BorderWidths(1.5)
            )));
        }

        if (canvasWrapperRef != null) {
            canvasWrapperRef.setBackground(new Background(
                    new BackgroundFill(
                            p.canvasBackground(),
                            new CornerRadii(16),
                            Insets.EMPTY
                    )
            ));
            canvasWrapperRef.setBorder(new Border(new BorderStroke(
                    p.canvasBorder(),
                    BorderStrokeStyle.SOLID,
                    new CornerRadii(16),
                    new BorderWidths(1.5)
            )));
        }

        if (bottomBoxRef != null) {
            bottomBoxRef.setBackground(new Background(
                    new BackgroundFill(
                            p.bottomBackground(),
                            new CornerRadii(14),
                            Insets.EMPTY
                    )
            ));
        }

        if (connectionLabel != null) {
            connectionLabel.setTextFill(p.textAccent());
        }
    }

    private void returnToMainMenu() {
        try {
            if (client != null) {
                client.disconnect();
                client = null;
            }
        } catch (Exception ignored) {}

        paused = false;
        completedCount = 0;
        levelCompletedShown = false;

        viewState.setClientId(null);
        viewState.setFishPosition(null);
        viewState.setCrabPosition(null);
        viewState.setObjects(Collections.emptyList());
        viewState.setLevelCompleted(false);

        if (gamePane != null) gamePane.setVisible(false);
        if (menuPane != null) menuPane.setVisible(true);

        if (connectionLabel != null) connectionLabel.setText("Not connected");
        if (roleLabel != null) roleLabel.setText("Role: —");
        if (pauseLabel != null) pauseLabel.setText("");
        if (completedLabel != null) completedLabel.setText("Completed: 0");
        if (logArea != null) logArea.clear();
        if (menuStatusLabel != null) menuStatusLabel.setText("");
    }

    private void onRestartClicked() {
        if (client == null || viewState.getClientId() == null) {
            appendLog("Not connected");
            return;
        }

        if (viewState.isLevelCompleted()) {
            appendLog("Level already completed – используйте голосование/переход после победы.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Restart level");
        confirm.setHeaderText("Запросить перезапуск уровня?");
        confirm.setContentText("Партнёру придёт запрос, и он сможет согласиться или отказаться.");

        ButtonType sendBtn = new ButtonType("Request restart", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(sendBtn, cancelBtn);

        confirm.showAndWait().ifPresent(bt -> {
            if (bt == sendBtn) {
                client.send(new RestartRequestMessage(viewState.getClientId()));
                appendLog("You asked partner to restart the level.");
            }
        });
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
