package symbiosis.common.net;

public class ProtocolDecoder {

    public Message decode(String line) throws IllegalArgumentException {
        if (line == null || line.isEmpty()) {
            throw new IllegalArgumentException("Empty line");
        }

        String[] parts = splitPreserveEscapes(line);
        String typeStr = parts[0];

        return switch (typeStr) {
            case "JOIN" -> {
                String name = parts.length > 1 ? unescape(parts[1]) : "Player";
                String prefRole = null;
                if (parts.length > 2 && !parts[2].isEmpty()) {
                    prefRole = parts[2];
                }
                int prefLevel = -1;
                if (parts.length > 3 && !parts[3].isEmpty()) {
                    try {
                        prefLevel = Integer.parseInt(parts[3]);
                    } catch (NumberFormatException ignored) {}
                }
                yield new JoinMessage(name, prefRole, prefLevel);
            }
            case "CHAT" -> {
                if (parts.length < 3) throw new IllegalArgumentException("Invalid CHAT");
                yield new ChatMessage(unescape(parts[1]), unescape(parts[2]));
            }
            case "ROLE_ASSIGNED" -> {
                if (parts.length < 3) throw new IllegalArgumentException("Invalid ROLE_ASSIGNED");
                yield new RoleAssignedMessage(unescape(parts[1]), unescape(parts[2]));
            }
            case "ERROR" -> {
                if (parts.length < 3) throw new IllegalArgumentException("Invalid ERROR");
                yield new ErrorMessage(unescape(parts[1]), unescape(parts[2]));
            }
            case "INPUT" -> {
                if (parts.length < 3) throw new IllegalArgumentException("Invalid INPUT");
                String clientId = unescape(parts[1]);
                InputMessage.InputType inputType = InputMessage.InputType.valueOf(parts[2]);
                yield new InputMessage(clientId, inputType);
            }
            case "STATE_UPDATE" -> {
                if (parts.length < 2) throw new IllegalArgumentException("Invalid STATE_UPDATE");
                yield new StateUpdateMessage(unescape(parts[1]));
            }
            case "LEVEL_DATA" -> {
                if (parts.length < 3) {
                    throw new IllegalArgumentException("Invalid LEVEL_DATA header");
                }
                int width = Integer.parseInt(parts[1]);
                int height = Integer.parseInt(parts[2]);
                if (parts.length < 3 + height) {
                    throw new IllegalArgumentException("Invalid LEVEL_DATA rows count");
                }
                String[] rows = new String[height];
                for (int i = 0; i < height; i++) {
                    rows[i] = parts[3 + i];
                }
                yield new LevelDataMessage(width, height, rows);
            }
            default -> throw new IllegalArgumentException("Unknown message type: " + typeStr);
        };
    }

    private String[] splitPreserveEscapes(String line) {
        java.util.List<String> result = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escape = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escape) {
                current.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '|') {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    private String unescape(String s) {
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escape) {
                sb.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
