package symbiosis.common.net;

public class RoleAssignedMessage extends Message {
    private final String playerId;
    private final String role;

    public RoleAssignedMessage(String playerId, String role) {
        super(MessageType.ROLE_ASSIGNED);
        this.playerId = playerId;
        this.role = role;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getRole() {
        return role;
    }
}
