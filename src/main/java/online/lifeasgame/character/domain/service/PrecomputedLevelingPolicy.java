package online.lifeasgame.character.domain.service;

import online.lifeasgame.character.domain.LevelingPolicyParameters;
import online.lifeasgame.character.domain.LevelingPolicyParameters.Bracket;
import online.lifeasgame.character.domain.error.LevelingError;
import online.lifeasgame.core.error.ConfigException;

import java.util.List;

public final class PrecomputedLevelingPolicy implements LevelingPolicy {

    private final int max;
    private final long[] req;
    private final long[] start;

    public PrecomputedLevelingPolicy(LevelingPolicyParameters parameters) {
        this.max = parameters.maxLevel();
        this.req = new long[max + 1];
        this.start = new long[max + 2];

        req[1] = parameters.baseReqLv1();
        start[1] = 0L;

        for (int l = 1; l <= max; l++) {
            if (l > 1) {
                var b = findBracket(parameters.brackets(), l);
                long candidate = (long) Math.floor(req[l - 1] * b.mul()) + b.add();
                req[l] = Math.max(candidate, req[l - 1] + 1);
            }
            start[l + 1] = start[l] + req[l];

            if (start[l + 1] < 0) {
                throw new ConfigException(LevelingError.LEVEL_CURVE_OVERFLOW, String.valueOf(l));
            }
        }
    }

    private static LevelingPolicyParameters.Bracket findBracket(List<Bracket> bs, int level) {
        for (var b : bs) {
            if (level >= b.from() && level <= b.to()) {
                return b;
            }
        }
        return bs.getLast();
    }

    @Override
    public int maxLevel() {
        return max;
    }

    @Override
    public long requiredExpFor(int level) {
        return (level >= max) ? 0 : req[Math.max(1, level)];
    }

    @Override
    public long totalXpAtLevelStart(int level) {
        return start[Math.max(1, Math.min(level, max + 1))];
    }

    @Override
    public int levelFor(long totalXp) {
        if (totalXp <= 0) {
            return 1;
        }

        int lo = 1, hi = max;

        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (totalXp < start[mid + 1]) {
                hi = mid - 1;
            } else {
                lo = mid + 1;
            }
        }
        return Math.max(1, Math.min(lo, max));
    }
}
