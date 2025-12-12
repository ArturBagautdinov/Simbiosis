package symbiosis.common.model;

public class Player {

    private String id;
    private String name;
    private PlayerRole role;
    private Position position;
    private boolean connected;

    public Player(String id, String name, PlayerRole role, Position position) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.position = position;
        this.connected = true;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public PlayerRole getRole() {
        return role;
    }

    public void setRole(PlayerRole role) {
        this.role = role;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
