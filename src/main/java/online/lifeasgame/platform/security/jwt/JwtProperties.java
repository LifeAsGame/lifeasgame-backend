package online.lifeasgame.platform.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "lifeasgame.jwt")
public class JwtProperties {
    private String secret;
    private long accessTokenExpiryMs  = 3_600_000L;
    private long refreshTokenExpiryMs = 604_800_000L;
}
