package online.lifeasgame.social.application.command;

public final class FollowCommand {

    private FollowCommand() {
    }

    public record Create(Long targetPlayerId) {
    }
}
