package online.lifeasgame.system.bootstrap.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.List;

@ConfigurationProperties(prefix = "lifeasgame.web.cors")
public record WebCorsProperties(List<String> allowedOrigins) {

    public WebCorsProperties {
        allowedOrigins = allowedOrigins == null
                ? List.of()
                : allowedOrigins.stream().map(String::trim).toList();

        if (allowedOrigins.isEmpty() || allowedOrigins.stream().anyMatch(origin ->
                origin.isEmpty() || origin.contains("*") || !isExactOrigin(origin))) {
            throw new IllegalArgumentException(
                    "lifeasgame.web.cors.allowed-origins requires exact "
                            + "http(s)://host[:port] origins"
            );
        }
    }

    private static boolean isExactOrigin(String origin) {
        try {
            URI uri = URI.create(origin);
            return ("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null
                    && uri.getRawUserInfo() == null
                    && (uri.getRawPath() == null || uri.getRawPath().isEmpty())
                    && uri.getRawQuery() == null
                    && uri.getRawFragment() == null
                    && uri.getPort() <= 65_535;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
