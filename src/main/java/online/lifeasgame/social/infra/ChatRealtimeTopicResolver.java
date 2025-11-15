package online.lifeasgame.social.infra;

import org.springframework.stereotype.Component;

@Component
public class ChatRealtimeTopicResolver {

    private static final String TOPIC_PREFIX = "social:chat:";
    private static final String DESTINATION_PREFIX = "/topic/social/chat/";

    public String topic(Long channelId) {
        return TOPIC_PREFIX + channelId;
    }

    public String pattern() {
        return TOPIC_PREFIX + "*";
    }

    public String destination(Long channelId) {
        return DESTINATION_PREFIX + channelId;
    }
}
