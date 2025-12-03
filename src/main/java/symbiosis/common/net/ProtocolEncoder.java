package symbiosis.common.net;

public class ProtocolEncoder {

    public String encode(Message msg) {
        switch (msg.getType()) {
            case JOIN -> {
                JoinMessage m = (JoinMessage) msg;
                return "JOIN|" + escape(m.getPlayerName());
            }
            case CHAT -> {
                ChatMessage m = (ChatMessage) msg;
                return "CHAT|" + escape(m.getFrom()) + "|" + escape(m.getText());
            }
            case ROLE_ASSIGNED -> {
                RoleAssignedMessage m = (RoleAssignedMessage) msg;
                return "ROLE_ASSIGNED|" + escape(m.getPlayerId()) + "|" + escape(m.getRole());
            }
            case ERROR -> {
                ErrorMessage m = (ErrorMessage) msg;
                return "ERROR|" + escape(m.getErrorCode()) + "|" + escape(m.getErrorText());
            }
            case INPUT -> {
                InputMessage m = (InputMessage) msg;
                return "INPUT|" + escape(m.getClientId()) + "|" + m.getInputType().name();
            }
            case STATE_UPDATE -> {
                StateUpdateMessage m = (StateUpdateMessage) msg;
                return "STATE_UPDATE|" + escape(m.getPayload());
            }
            case LEVEL_DATA -> {
                LevelDataMessage m = (LevelDataMessage) msg;
                StringBuilder sb = new StringBuilder();
                sb.append("LEVEL_DATA|")
                  .append(m.getWidth())
                  .append("|")
                  .append(m.getHeight());
                for (String row : m.getRows()) {
                    sb.append("|").append(row);
                }
                return sb.toString();
            }
            default -> throw new IllegalArgumentException("Unknown message type: " + msg.getType());
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("|", "\\|");
    }
}
