package online.lifeasgame.character.infra.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import online.lifeasgame.character.domain.LevelingPolicyParameters;
import online.lifeasgame.character.domain.error.LevelingError;
import online.lifeasgame.core.error.ConfigException;
import online.lifeasgame.core.guard.Guard;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "lag.level-curve")
public record LevelCurveProperties(
        @Min(1) int maxLevel,
        @Min(1) long baseReqLv1,
        List<@Valid Bracket> brackets
) {

    public LevelCurveProperties {
        brackets = List.copyOf(Guard.notNull(brackets, "brackets"));
        for (Bracket bracket : brackets) {
            if (bracket.from() > bracket.to()) {
                throw new ConfigException(LevelingError.INVALID_BRACKETS, "from > to: " + bracket.from() + ">" + bracket.to());
            }
            if (!Double.isFinite(bracket.mul())) {
                throw new ConfigException(LevelingError.INVALID_BRACKETS, "non-finite mul at [" + bracket.from() + "," + bracket.to() + "]");
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
                .map(
                        bracket -> new LevelingPolicyParameters.Bracket(
                                bracket.from(),
                                bracket.to(),
                                bracket.mul(),
                                bracket.add()
                        )
                )
                .toList();
        return new LevelingPolicyParameters(maxLevel, baseReqLv1, list);
    }
}
