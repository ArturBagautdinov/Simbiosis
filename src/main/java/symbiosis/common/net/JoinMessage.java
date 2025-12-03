package symbiosis.common.net;

public class JoinMessage extends Message {
    private final String playerName;

    public JoinMessage(String playerName) {
        super(MessageType.JOIN);
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }
}
