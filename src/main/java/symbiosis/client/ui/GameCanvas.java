package symbiosis.client.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import symbiosis.common.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameCanvas extends Canvas {

    private final ViewState viewState;
    private final int tileSize = 40;

    private final int fishLightRadiusTiles = 4;
    private final int mushroomLightRadiusTiles = 3;

    private double animTime = 0.0;
    private double lastDt = 0.016;

    private Double fishScreenX = null;
    private Double fishScreenY = null;
    private Double crabScreenX = null;
    private Double crabScreenY = null;

    private final Map<Integer, Double> boxScreenX = new HashMap<>();
    private final Map<Integer, Double> boxScreenY = new HashMap<>();

    private final AnimationTimer timer;

    private final Textures textures = new Textures();

    public GameCanvas(double width, double height, ViewState viewState) {
        super(width, height);
        this.viewState = viewState;

        timer = new AnimationTimer() {
            private long lastTime = -1;

            @Override
            public void handle(long now) {
                if (lastTime < 0) {
                    lastTime = now;
                    return;
                }
                double dt = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;

                animTime += dt;
                lastDt = dt;
                updatePositions(dt);
                render();
            }
        };
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    private void updatePositions(double dt) {
        Position fish = viewState.getFishPosition();
        if (fish != null) {
            double targetX = fish.getX() * tileSize + tileSize / 2.0;
            double targetY = fish.getY() * tileSize + tileSize / 2.0;

            if (fishScreenX == null || fishScreenY == null) {
                fishScreenX = targetX;
                fishScreenY = targetY;
            } else {
                double lerpFactor = 10.0 * dt;
                lerpFactor = Math.min(1.0, lerpFactor);
                fishScreenX = fishScreenX + (targetX - fishScreenX) * lerpFactor;
                fishScreenY = fishScreenY + (targetY - fishScreenY) * lerpFactor;
            }
        }

        Position crab = viewState.getCrabPosition();
        if (crab != null) {
            double targetX = crab.getX() * tileSize + tileSize / 2.0;
            double targetY = crab.getY() * tileSize + tileSize / 2.0;

            if (crabScreenX == null || crabScreenY == null) {
                crabScreenX = targetX;
                crabScreenY = targetY;
            } else {
                double lerpFactor = 10.0 * dt;
                lerpFactor = Math.min(1.0, lerpFactor);
                crabScreenX = crabScreenX + (targetX - crabScreenX) * lerpFactor;
                crabScreenY = crabScreenY + (targetY - crabScreenY) * lerpFactor;
            }
        }
    }

    public void render() {
        GraphicsContext gc = getGraphicsContext2D();

        double w = getWidth();
        double h = getHeight();

        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, w, h);

        TileType[][] tiles = viewState.getTiles();
        int mapW = viewState.getMapWidth();
        int mapH = viewState.getMapHeight();

        if (tiles == null || mapW == 0 || mapH == 0) {
            return;
        }

        SkinTheme theme = viewState.getSkinTheme();
        Color emptyColor;
        Color gridColor;
        Color glowColor;

        switch (theme) {
            case OCEAN -> {
                emptyColor = Color.rgb(5, 10, 30);
                gridColor = Color.color(1, 1, 1, 0.06);
                glowColor = Color.LIMEGREEN;
            }
            case DEEP -> {
                emptyColor = Color.rgb(2, 2, 10);
                gridColor = Color.color(0.8, 0.8, 1, 0.05);
                glowColor = Color.TURQUOISE;
            }
            default -> {
                emptyColor = Color.rgb(15, 15, 25);
                gridColor = Color.color(1, 1, 1, 0.08);
                glowColor = Color.LIMEGREEN;
            }
        }

        PlayerRole role = viewState.getLocalRole();
        boolean isCrab = (role == PlayerRole.CRAB);

        Position fishPos = viewState.getFishPosition();
        List<GameObject> objects = viewState.getObjects();

        for (int y = 0; y < mapH; y++) {
            for (int x = 0; x < mapW; x++) {
                boolean lit = !isCrab || isTileLitForCrab(x, y, fishPos, objects);

                if (!lit && isCrab) {
                    continue;
                }

                TileType tile = tiles[y][x];

                double px = x * tileSize;
                double py = y * tileSize;

                switch (tile) {
                    case WALL -> drawTile(gc, textures.wall, px, py);
                    case EXIT -> {
                        drawTile(gc, textures.floorLight, px, py);
                        drawTile(gc, textures.exit, px, py);

                        double pulse = 0.5 + 0.5 * Math.sin(animTime * 3.0);
                        gc.setGlobalAlpha(0.3 + 0.3 * pulse);
                        gc.setFill(glowColor);
                        gc.fillOval(px - tileSize * 0.1, py - tileSize * 0.1,
                                tileSize * 1.2, tileSize * 1.2);
                        gc.setGlobalAlpha(1.0);
                    }
                    case LIGHT_TILE -> drawTile(gc, textures.floorLight, px, py);
                    case DARK_TILE -> drawTile(gc, textures.floorDark, px, py);
                    case EMPTY -> drawTile(gc, textures.floor, px, py);
                }

                gc.setStroke(gridColor);
                gc.strokeRect(px, py, tileSize, tileSize);
            }
        }

        if (objects != null) {
            for (int i = 0; i < objects.size(); i++) {
                GameObject obj = objects.get(i);
                Position op = obj.getPosition();
                int ox = op.getX();
                int oy = op.getY();

                boolean lit = !isCrab || isTileLitForCrab(ox, oy, fishPos, objects);
                if (!lit && isCrab) continue;

                double px = ox * tileSize;
                double py = oy * tileSize;

                switch (obj.getType()) {
                    case BOX -> {
                        double targetX = px + tileSize / 2.0;
                        double targetY = py + tileSize / 2.0;

                        Double sx = boxScreenX.get(i);
                        Double sy = boxScreenY.get(i);

                        if (sx == null || sy == null) {
                            sx = targetX;
                            sy = targetY;
                        } else {
                            double lerpFactor = 10.0 * lastDt;
                            lerpFactor = Math.min(1.0, lerpFactor);
                            sx = sx + (targetX - sx) * lerpFactor;
                            sy = sy + (targetY - sy) * lerpFactor;
                        }

                        boxScreenX.put(i, sx);
                        boxScreenY.put(i, sy);

                        gc.drawImage(
                                textures.box,
                                sx - tileSize / 2.0,
                                sy - tileSize / 2.0,
                                tileSize,
                                tileSize
                        );
                    }
                    case ROCK -> {
                        gc.setFill(Color.GRAY);
                        gc.fillOval(px + tileSize * 0.2, py + tileSize * 0.2,
                                tileSize * 0.6, tileSize * 0.6);
                    }
                    case MUSHROOM -> {
                        if (obj.isActive()) {
                            double phase = ox + oy;
                            double flicker = 0.5 + 0.5 * Math.sin(animTime * 5.0 + phase);
                            double alpha = 0.3 + 0.3 * flicker;

                            gc.setGlobalAlpha(alpha);
                            gc.setFill(Color.GOLD);
                            gc.fillOval(px - tileSize * 0.2, py - tileSize * 0.2,
                                    tileSize * 1.4, tileSize * 1.4);
                            gc.setGlobalAlpha(1.0);
                        }

                        gc.drawImage(textures.mushroom, px, py, tileSize, tileSize);
                    }
                }
            }
        }

        Position fish = viewState.getFishPosition();
        if (fish != null && fishScreenX != null && fishScreenY != null) {
            boolean lit = !isCrab || isTileLitForCrab(fish.getX(), fish.getY(), fishPos, objects);
            if (!isCrab || lit) {
                gc.drawImage(
                        textures.fish,
                        fishScreenX - tileSize / 2.0,
                        fishScreenY - tileSize / 2.0,
                        tileSize,
                        tileSize
                );
            }
        }

        Position crab = viewState.getCrabPosition();
        if (crab != null && crabScreenX != null && crabScreenY != null) {
            boolean lit = !isCrab || isTileLitForCrab(crab.getX(), crab.getY(), fishPos, objects);
            if (!isCrab || lit) {
                gc.drawImage(
                        textures.crab,
                        crabScreenX - tileSize / 2.0,
                        crabScreenY - tileSize / 2.0,
                        tileSize,
                        tileSize
                );
            }
        }
    }

    private void drawTile(GraphicsContext gc, Image image, double px, double py) {
        gc.drawImage(image, px, py, tileSize, tileSize);
    }

    private boolean isTileLitForCrab(int tileX, int tileY,
                                     Position fishPos,
                                     List<GameObject> objects) {
        if (fishPos != null) {
            double distFish = distanceTiles(tileX, tileY, fishPos.getX(), fishPos.getY());
            if (distFish <= fishLightRadiusTiles) {
                return true;
            }
        }

        if (objects != null) {
            for (GameObject obj : objects) {
                if (obj.getType() == ObjectType.MUSHROOM && obj.isActive()) {
                    Position p = obj.getPosition();
                    double distM = distanceTiles(tileX, tileY, p.getX(), p.getY());
                    if (distM <= mushroomLightRadiusTiles) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private double distanceTiles(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
