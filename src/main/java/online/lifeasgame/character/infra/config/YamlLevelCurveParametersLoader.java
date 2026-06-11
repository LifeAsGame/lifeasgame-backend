package online.lifeasgame.character.infra.config;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.LevelingPolicyParameters;
import online.lifeasgame.character.domain.repository.LevelCurveParametersLoader;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(LevelCurveProperties.class)
public class YamlLevelCurveParametersLoader implements LevelCurveParametersLoader {

    private final LevelCurveProperties levelCurveProperties;

    @Override
    public LevelingPolicyParameters load() {
        return levelCurveProperties.toParams();
    }
}
