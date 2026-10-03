package online.lifeasgame.lifelog.application.internal;

public interface LifeLogEvidenceReadApi {
    Evidence requireContentfulOwned(Long playerId, Long lifeLogId);

    record Evidence(Long lifeLogId, Long primaryRoleId, boolean projectCollection) {
    }
}
