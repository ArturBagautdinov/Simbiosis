package symbiosis.common.net;

public class StateUpdateMessage extends Message {
    private final String payload; 

    public StateUpdateMessage(String payload) {
        super(MessageType.STATE_UPDATE);
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }
}
