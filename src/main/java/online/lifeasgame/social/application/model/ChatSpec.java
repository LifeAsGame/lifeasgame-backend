package online.lifeasgame.social.application.model;

import online.lifeasgame.social.application.command.ChatCommand;

public final class ChatSpec {

    private ChatSpec() {
    }

    public record OpenGlobal(String name) {
        public static OpenGlobal from(ChatCommand.OpenGlobal command) {
            return new OpenGlobal(command == null ? null : command.name());
        }
    }

    public record OpenFriend(Long playerId, Long friendId, String name) {
        private static String sanitize(String name) {
            return (name == null || name.isBlank()) ? "친구 채팅" : name;
        }

        public static OpenFriend from(Long playerId, Long friendId, ChatCommand.OpenFriend command) {
            return new OpenFriend(playerId, friendId, sanitize(command == null ? null : command.name()));
        }
    }

    public record OpenAdmin(Long actorId, Long contextId, String name) {
        private static String sanitize(String name) {
            return (name == null || name.isBlank()) ? "운영진 문의" : name;
        }

        public static OpenAdmin forPlayer(Long playerId, ChatCommand.OpenAdmin command) {
            return new OpenAdmin(playerId, playerId, sanitize(command == null ? null : command.name()));
        }

        public static OpenAdmin forOperator(Long operatorId, Long targetPlayerId, ChatCommand.OpenAdmin command) {
            return new OpenAdmin(operatorId, targetPlayerId, sanitize(command == null ? null : command.name()));
        }
    }

    public record SendMessage(Long channelId, Long senderId, String content) {
        public static SendMessage from(Long channelId, Long senderId, ChatCommand.SendMessage command) {
            return new SendMessage(channelId, senderId, command.content());
        }
    }
}
