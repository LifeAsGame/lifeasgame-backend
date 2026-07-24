package online.lifeasgame.platform.outbox.application;

public record OutboxRelayResult(
        int recovered,
        int claimed,
        int published,
        int failed
) {
}
