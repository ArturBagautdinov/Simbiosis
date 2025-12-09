package symbiosis.common.net;

public class RestartOfferMessage extends Message {

    private final String fromName;

    public RestartOfferMessage(String fromName) {
        super(MessageType.RESTART_OFFER);
        this.fromName = fromName;
    }

    public String getFromName() {
        return fromName;
    }
}
