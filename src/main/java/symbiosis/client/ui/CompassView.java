package symbiosis.client.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import symbiosis.common.model.Position;

public class CompassView extends Canvas {

    private final ViewState viewState;

    private double currentAngleRad = 0.0;

    private final AnimationTimer timer;

    public CompassView(double size, ViewState viewState) {
        super(size, size);
        this.viewState = viewState;

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateAngle();
                render();
            }
        };
        timer.start();
    }

    private void updateAngle() {
        Position local = viewState.getLocalPlayerPosition();
        Position partner = viewState.getPartnerPosition();
        if (local == null || partner == null) {
            double target = 0.0;
            currentAngleRad += (target - currentAngleRad) * 0.1;
            return;
        }

        double dx = partner.getX() - local.getX();
        double dy = partner.getY() - local.getY();

        if (dx == 0 && dy == 0) {
            return;
        }

        double targetAngle = Math.atan2(dy, dx);

        double diff = normalizeAngle(targetAngle - currentAngleRad);
        currentAngleRad = currentAngleRad + diff * 0.1;
    }

    private double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    private void render() {
        double w = getWidth();
        double h = getHeight();
        double cx = w / 2.0;
        double cy = h / 2.0;
        double radius = Math.min(w, h) * 0.45;

        GraphicsContext gc = getGraphicsContext2D();

        gc.setFill(Color.color(0, 0, 0, 0.6));
        gc.fillRect(0, 0, w, h);

        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(2);
        gc.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);

        double arrowLen = radius * 0.8;
        double angle = currentAngleRad;

        double x2 = cx + Math.cos(angle) * arrowLen;
        double y2 = cy + Math.sin(angle) * arrowLen;

        gc.setStroke(Color.GOLD);
        gc.setLineWidth(3);
        gc.strokeLine(cx, cy, x2, y2);

        double headSize = 8;
        double leftAngle = angle + Math.toRadians(150);
        double rightAngle = angle - Math.toRadians(150);

        double xLeft = x2 + Math.cos(leftAngle) * headSize;
        double yLeft = y2 + Math.sin(leftAngle) * headSize;
        double xRight = x2 + Math.cos(rightAngle) * headSize;
        double yRight = y2 + Math.sin(rightAngle) * headSize;

        gc.setFill(Color.GOLD);
        gc.fillPolygon(
                new double[]{x2, xLeft, xRight},
                new double[]{y2, yLeft, yRight},
                3
        );

        gc.setFill(Color.DARKGRAY);
        gc.fillOval(cx - 4, cy - 4, 8, 8);
    }

    public void stop() {
        timer.stop();
    }
}
