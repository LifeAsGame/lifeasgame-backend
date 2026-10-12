package online.lifeasgame.demo.application;

public interface DemoActorScopeApi {
    boolean sameBoundary(Long actorPlayerId, Long targetPlayerId);
    void requireSameBoundary(Long actorPlayerId, Long targetPlayerId);
    void requireActive(Long userId, Long playerId);
}
