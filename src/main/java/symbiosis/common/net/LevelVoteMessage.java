package symbiosis.common.net;

public class LevelVoteMessage extends Message {

    private final String clientId;
    private final int levelIndex;

    public LevelVoteMessage(String clientId, int levelIndex) {
        super(MessageType.LEVEL_VOTE);
        this.clientId = clientId;
        this.levelIndex = levelIndex;
    }

    public String getClientId() {
        return clientId;
    }

    public int getLevelIndex() {
        return levelIndex;
    }
}
