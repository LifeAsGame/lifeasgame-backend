package online.lifeasgame.system.bootstrap.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "lifeasgame.web.cors")
public record WebCorsProperties(List<String> allowedOrigins) {

    public WebCorsProperties {
        allowedOrigins = allowedOrigins == null
                ? List.of()
                : allowedOrigins.stream().map(String::trim).toList();

        if (allowedOrigins.isEmpty()
                || allowedOrigins.stream().anyMatch(origin -> origin.isEmpty() || origin.contains("*"))) {
            throw new IllegalArgumentException(
                    "lifeasgame.web.cors.allowed-origins requires explicit origins without wildcards"
            );
        }
    }
}
