package online.lifeasgame.quest.domain;

import java.time.Instant;

public record JourneyEvidence(
        Long acceptanceId,
        String kind,
        Long lifeLogId,
        String memo,
        String url,
        String description,
        Instant linkedAt
) {
    public JourneyEvidence {
        boolean valid = switch (kind) {
            case "GOAL_MEMO" -> lifeLogId == null && memo != null && url == null && description == null;
            case "LIFE_LOG", "PROJECT" -> lifeLogId != null && lifeLogId > 0 && memo == null && url == null && description == null;
            case "DEPLOYMENT" -> lifeLogId == null && memo == null && url != null && description != null;
            default -> false;
        };
        if (acceptanceId == null || acceptanceId <= 0 || linkedAt == null || !valid) {
            throw new IllegalArgumentException("Invalid journey evidence state");
        }
    }

    public boolean sameContent(JourneyEvidence other) {
        return kind.equals(other.kind)
                && java.util.Objects.equals(lifeLogId, other.lifeLogId)
                && java.util.Objects.equals(memo, other.memo)
                && java.util.Objects.equals(url, other.url)
                && java.util.Objects.equals(description, other.description);
    }
}
