package symbiosis.common.net;

public class ErrorMessage extends Message {
    private final String errorCode;
    private final String errorText;

    public ErrorMessage(String errorCode, String errorText) {
        super(MessageType.ERROR);
        this.errorCode = errorCode;
        this.errorText = errorText;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorText() {
        return errorText;
    }
}
