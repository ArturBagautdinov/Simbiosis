package symbiosis.common.net;

public class RestartResponseMessage extends Message {

    private final String clientId;
    private final boolean accepted;

    public RestartResponseMessage(String clientId, boolean accepted) {
        super(MessageType.RESTART_RESPONSE);
        this.clientId = clientId;
        this.accepted = accepted;
    }

    public String getClientId() {
        return clientId;
    }

    public boolean isAccepted() {
        return accepted;
    }
}
