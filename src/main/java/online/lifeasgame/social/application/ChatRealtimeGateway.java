package online.lifeasgame.social.application;

public interface ChatRealtimeGateway {

    void publish(ChatRealtimePayload payload);

    void publishRead(ChatReadRealtimePayload payload);
}
