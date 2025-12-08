package symbiosis.client.ui;

import javafx.scene.image.Image;

public class Textures {

    public final Image wall;
    public final Image floorDark;
    public final Image floorLight;
    public final Image exit;
    public final Image mushroom;
    public final Image box;
    public final Image crab;
    public final Image fish;
    public final Image floor;

    public Textures() {
        wall       = load("textures/wall.png");
        floor      = load("textures/floor.png");
        floorDark  = load("textures/floor_dark.png");
        floorLight = load("textures/floor_light.png");
        exit       = load("textures/exit.png");
        mushroom   = load("textures/mushroom.png");
        box        = load("textures/box.png");
        crab       = load("textures/crab.png");
        fish       = load("textures/fish.png");
    }

    private Image load(String path) {
        var url = getClass().getClassLoader().getResource(path);
        if (url == null) {
            throw new RuntimeException("Texture not found: " + path);
        }
        return new Image(url.toExternalForm());
    }
}
