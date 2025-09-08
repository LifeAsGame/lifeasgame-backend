package online.lifeasgame.character.infra.config;

import java.util.List;
import online.lifeasgame.character.domain.LevelingPolicyParameters;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lag.level-curve")
public record LevelCurveProperties(
        int maxLevel,
        long baseReqLv1,
        List<Bracket> brackets
) {
    public record Bracket(int from, int to, double mul, long add) {}

    public LevelingPolicyParameters toParams() {
        var list = brackets.stream()
                .map(b -> new LevelingPolicyParameters.Bracket(b.from(), b.to(), b.mul(), b.add()))
                .toList();
        return new LevelingPolicyParameters(maxLevel, baseReqLv1, list);
    }
}
