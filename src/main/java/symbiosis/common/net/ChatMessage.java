package symbiosis.common.net;

public class ChatMessage extends Message {
    private final String from;
    private final String text;

    public ChatMessage(String from, String text) {
        super(MessageType.CHAT);
        this.from = from;
        this.text = text;
    }

    public String getFrom() {
        return from;
    }

    public String getText() {
        return text;
    }
}
