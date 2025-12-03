package symbiosis.common.model;

public class CaveMap {
    private final int width;
    private final int height;
    private final TileType[][] tiles;

    public CaveMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new TileType[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles[y][x] = TileType.EMPTY;
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public TileType getTile(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return TileType.WALL;
        }
        return tiles[y][x];
    }

    public void setTile(int x, int y, TileType type) {
        tiles[y][x] = type;
    }
}
