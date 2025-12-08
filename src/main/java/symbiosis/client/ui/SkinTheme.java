package symbiosis.client.ui;


import javafx.scene.paint.Color;

public enum SkinTheme {
    CLASSIC,
    OCEAN,
    DEEP;

    public ThemePalette palette() {
        return switch (this) {
            case CLASSIC -> new ThemePalette(

                    Color.rgb(10, 20, 40, 0.92),
                    Color.rgb(80, 150, 255, 0.6),
                    Color.rgb(6, 10, 22, 0.92),
                    Color.rgb(8, 16, 35, 0.94),
                    Color.rgb(70, 120, 200, 0.7),
                    Color.rgb(3, 8, 20, 0.9),
                    Color.rgb(40, 80, 150, 0.8),
                    Color.rgb(5, 10, 24, 0.94),
                    Color.web("#e7f5ff"),
                    Color.web("#ffd49e")
            );
            case OCEAN -> new ThemePalette(
                    Color.rgb(0, 40, 70, 0.9),
                    Color.rgb(64, 196, 255, 0.8),

                    Color.rgb(0, 22, 40, 0.95),

                    Color.rgb(0, 28, 54, 0.96),
                    Color.rgb(64, 196, 255, 0.9),

                    Color.rgb(0, 18, 36, 0.95),
                    Color.rgb(0, 120, 200, 0.85),

                    Color.rgb(0, 18, 32, 0.95),

                    Color.web("#e0f7ff"),
                    Color.web("#ffecb3")
            );
            case DEEP -> new ThemePalette(
                    Color.rgb(16, 10, 40, 0.95),
                    Color.rgb(150, 110, 255, 0.8),

                    Color.rgb(8, 4, 24, 0.96),

                    Color.rgb(12, 8, 36, 0.96),
                    Color.rgb(160, 120, 255, 0.85),

                    Color.rgb(6, 4, 20, 0.95),
                    Color.rgb(120, 80, 220, 0.85),

                    Color.rgb(8, 6, 26, 0.95),

                    Color.web("#f0e9ff"),
                    Color.web("#ffb3e6")
            );
        };
    }

    public record ThemePalette(
            Color menuCardBackground,
            Color menuCardBorder,
            Color topBarBackground,
            Color hudBackground,
            Color hudBorder,
            Color canvasBackground,
            Color canvasBorder,
            Color bottomBackground,
            Color textMain,
            Color textAccent
    ) {}
}
