package online.lifeasgame.character.infra.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.util.List;
import online.lifeasgame.character.domain.LevelingPolicyParameters;
import online.lifeasgame.character.domain.error.LevelingError;
import online.lifeasgame.core.error.ConfigException;
import online.lifeasgame.core.guard.Guard;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "lag.level-curve")
public record LevelCurveProperties(
        @Min(1) int maxLevel,
        @Min(1) long baseReqLv1,
        List<@Valid Bracket> brackets
) {

    public LevelCurveProperties {
        brackets = List.copyOf(Guard.notNull(brackets, "brackets"));
        for (var b : brackets) {
            if (b.from() > b.to()) {
                throw new ConfigException(LevelingError.INVALID_BRACKETS, "from > to: " + b.from() + ">" + b.to());
            }
            if (!Double.isFinite(b.mul())) {
                throw new ConfigException(LevelingError.INVALID_BRACKETS, "non-finite mul at [" + b.from() + "," + b.to() + "]");
            }
        }
    }

    public record Bracket(
            @Min(1) int from,
            @Min(1) int to,
            @DecimalMin("0.0") double mul,
            @Min(0) long add
    ) {}

    public LevelingPolicyParameters toParams() {
        var list = brackets.stream()
                .map(b -> new LevelingPolicyParameters.Bracket(b.from(), b.to(), b.mul(), b.add()))
                .toList();
        return new LevelingPolicyParameters(maxLevel, baseReqLv1, list);
    }
}
