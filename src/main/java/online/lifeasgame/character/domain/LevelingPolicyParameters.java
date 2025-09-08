package online.lifeasgame.character.domain;

import java.util.List;
import online.lifeasgame.character.domain.error.LevelingError;
import online.lifeasgame.core.error.ConfigException;
import online.lifeasgame.core.guard.Guard;

public record LevelingPolicyParameters(
        int maxLevel,
        long baseReqLv1,
        List<Bracket> brackets
) {
    public LevelingPolicyParameters {
        Guard.minValue(maxLevel, 1, "maxLevel");
        Guard.minValue(baseReqLv1, 1, "baseReqLv1");
        brackets = List.copyOf(Guard.notNull(brackets, "brackets"));
        for (Bracket b : brackets) {
            if (b.from() < 1 || b.to() < b.from() || b.to() > maxLevel) {
                throw new ConfigException(LevelingError.INVALID_BRACKETS);
            }
        }
    }

    public record Bracket(int from, int to, double mul, long add) {
        public Bracket {
            if (!Double.isFinite(mul)) {
                throw new ConfigException(LevelingError.INVALID_BRACKETS, "mul must be finite");
            }
            Guard.minValue(mul, 0.0d, "mul");
            Guard.minValue(add, 0L, "add");
        }
    }
}
