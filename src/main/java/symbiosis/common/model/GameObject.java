package symbiosis.common.model;

public class GameObject {
    private ObjectType type;
    private Position position;
    private boolean active;

    public GameObject(ObjectType type, Position position) {
        this.type = type;
        this.position = position;
        this.active = false;
    }

    public ObjectType getType() {
        return type;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
