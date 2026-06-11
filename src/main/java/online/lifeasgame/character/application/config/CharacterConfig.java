package online.lifeasgame.character.application.config;

import online.lifeasgame.character.domain.repository.LevelCurveParametersLoader;
import online.lifeasgame.character.domain.service.LevelingPolicy;
import online.lifeasgame.character.domain.service.PrecomputedLevelingPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CharacterConfig {

    @Bean
    LevelingPolicy levelingPolicy(LevelCurveParametersLoader loader) {
        return new PrecomputedLevelingPolicy(loader.load());
    }
}
