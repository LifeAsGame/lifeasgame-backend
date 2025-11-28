package online.lifeasgame.quest.infra.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "lifeasgame.quest.events")
public class QuestEventProperties {

    private boolean enabled = false;

    private String topic = "quest.events";
}
