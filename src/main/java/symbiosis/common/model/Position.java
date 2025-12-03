package symbiosis.common.model;

public class Position {
    private int x;
    private int y;

    public Position() {
    }

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return x + "," + y;
    }

    public static Position fromString(String s) throws IllegalArgumentException {
        String[] parts = s.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid position: " + s);
        }
        return new Position(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
}
