package online.lifeasgame.social.application.model;

import online.lifeasgame.social.application.command.FollowCommand;

public final class FollowSpec {
    public record Create(Long playerId, Long targetPlayerId) {
        public static Create from(Long playerId, FollowCommand.Create c) {
            return new Create(playerId, c.targetPlayerId());
        }
    }
}

