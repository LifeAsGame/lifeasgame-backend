package online.lifeasgame.global.error.docs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "app.error.docs")
public class AppErrorDocsProperties {
    private String base = "";

    private String ext = "";

    public boolean isConfigured() { return base != null && !base.isBlank(); }
}
