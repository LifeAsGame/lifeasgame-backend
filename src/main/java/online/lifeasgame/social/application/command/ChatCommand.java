package online.lifeasgame.social.application.command;

public final class ChatCommand {

    private ChatCommand() {
    }

    public record OpenGlobal(String name) {
    }

    public record OpenFriend(String name) {
    }

    public record OpenAdmin(String name) {
    }

    public record SendMessage(String content) {
    }
}
