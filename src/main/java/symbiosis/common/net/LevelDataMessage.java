package symbiosis.common.net;

public class LevelDataMessage extends Message {

    private final int width;
    private final int height;
    private final String[] rows;

    public LevelDataMessage(int width, int height, String[] rows) {
        super(MessageType.LEVEL_DATA);
        this.width = width;
        this.height = height;
        this.rows = rows;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String[] getRows() {
        return rows;
    }
}
