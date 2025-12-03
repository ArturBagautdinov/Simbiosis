package symbiosis.common.net;

public class InputMessage extends Message {

    public enum InputType {
        MOVE_UP,
        MOVE_DOWN,
        MOVE_LEFT,
        MOVE_RIGHT,
        ACTION,
        STOP
    }

    private final String clientId;
    private final InputType inputType;

    public InputMessage(String clientId, InputType inputType) {
        super(MessageType.INPUT);
        this.clientId = clientId;
        this.inputType = inputType;
    }

    public String getClientId() {
        return clientId;
    }

    public InputType getInputType() {
        return inputType;
    }
}
