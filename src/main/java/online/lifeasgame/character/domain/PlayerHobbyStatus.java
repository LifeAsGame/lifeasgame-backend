package online.lifeasgame.character.domain;

import java.util.List;
import online.lifeasgame.character.domain.error.PlayerHobbyError;
import online.lifeasgame.core.lang.EnumParsers;

public enum PlayerHobbyStatus {
    ACTIVE, PAUSED, DROPPED
    ;

    public static PlayerHobbyStatus parse(String raw) {
        return EnumParsers.parseStrict(
                PlayerHobbyStatus.class,
                raw,
                PlayerHobbyError.INVALID_PLAYER_HOBBY_STATUS,
                "PlayerHobbyStatus"
        );
    }

    public static List<PlayerHobbyStatus> parse(List<String> raw) {
        return EnumParsers.parseListStrict(
                PlayerHobbyStatus.class,
                raw,
                PlayerHobbyError.INVALID_PLAYER_HOBBY_STATUS,
                "PlayerHobbyStatus"
        );
    }
}
