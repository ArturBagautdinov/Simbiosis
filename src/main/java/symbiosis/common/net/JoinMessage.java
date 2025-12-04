package symbiosis.common.net;

public class JoinMessage extends Message {

    private final String playerName;
    private final String preferredRole;
    private final int preferredLevel;

    public JoinMessage(String playerName) {
        this(playerName, null, -1);
    }

    public JoinMessage(String playerName, String preferredRole, int preferredLevel) {
        super(MessageType.JOIN);
        this.playerName = playerName;
        this.preferredRole = preferredRole;
        this.preferredLevel = preferredLevel;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getPreferredRole() {
        return preferredRole;
    }

    public int getPreferredLevel() {
        return preferredLevel;
    }
}
