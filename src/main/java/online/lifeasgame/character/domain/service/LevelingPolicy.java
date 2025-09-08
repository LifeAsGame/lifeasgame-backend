package online.lifeasgame.character.domain.service;

public interface LevelingPolicy {
    int maxLevel();
    long requiredExpFor(int level);
    long totalXpAtLevelStart(int level);

    default int levelFor(long totalXp) {
        int lv = 1;
        int max = maxLevel();

        while (lv < max && totalXp >= totalXpAtLevelStart(lv + 1)) {
            lv++;
        }

        return lv;
    }

    default Progress progressOf(long totalXp, int currentLevel) {
        int lv = Math.max(1, Math.min(maxLevel(), currentLevel));
        long cap = (lv >= maxLevel()) ? 0 : requiredExpFor(lv);
        long start = totalXpAtLevelStart(lv);
        long into = (lv >= maxLevel()) ? 0 : Math.max(0, totalXp - start);
        long toNext = (lv >= maxLevel()) ? 0 : Math.max(0, cap - into);
        double ratio = (lv >= maxLevel() || cap == 0) ? 1.0 : Math.min(1.0, ((double) into / (double) cap));
        return new Progress(lv, totalXp, into, toNext, cap, ratio);
    }

    record Progress(
            int level,
            long totalXp,
            long expIntoLevel,
            long expToNext,
            long capForLevel,
            double progressRatio
    ) {
    }
}
