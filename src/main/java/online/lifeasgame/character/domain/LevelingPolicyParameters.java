package online.lifeasgame.character.domain;

import java.util.List;

public record LevelingPolicyParameters(
        int maxLevel,
        long baseReqLv1,
        List<Bracket> brackets
) {
    public record Bracket(int from, int to, double mul, long add) {}
}
