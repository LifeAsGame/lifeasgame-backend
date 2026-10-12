package online.lifeasgame.demo.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lifeasgame.portfolio-demo")
public record DemoProperties(
        boolean enabled,
        int ttlHours,
        int maxActiveRuns,
        int maxDailyRuns,
        int maxTotalRuns
) {}
