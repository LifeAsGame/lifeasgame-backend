package online.lifeasgame.economy.infra.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "lifeasgame.economy.events")
public class EconomyEventProperties {

    private boolean enabled = false;
    private String topic = "economy.events";
    private String channel = "economy.events";
}
