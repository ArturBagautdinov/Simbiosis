package symbiosis.common.net;

public class RestartRequestMessage extends Message {

    private final String clientId;

    public RestartRequestMessage(String clientId) {
        super(MessageType.RESTART_REQUEST);
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }
}
