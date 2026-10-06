package online.lifeasgame.character.application;

import java.time.Instant;
import java.util.Optional;
import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.inventory.domain.event.ItemRewardClaimed;
import online.lifeasgame.lifelog.domain.event.LifeLogRecorded;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import online.lifeasgame.quest.domain.event.QuestRouteCompleted;

/** Explicit activation policy; description text never determines eligibility. */
final class AchievementActivationRule {
    private AchievementActivationRule() {}

    static Optional<EligibleFact> match(DomainEvent event) {
        if (event instanceof LifeLogRecorded fact && fact.isContentReady()) {
            return Optional.of(new EligibleFact(fact.playerId(), "ACH_FIRST_LIFELOG",
                    "LIFELOG", Long.toString(fact.lifeLogId()), fact.occurredAt(),
                    "TITLE_CANDIDATE_RECORD_BEGINNER"));
        }
        if (event instanceof QuestEvent fact
                && fact.type() == QuestEventType.QUEST_COMPLETED
                && fact.playerId() != null
                && fact.attributes().get("acceptanceId") instanceof Number acceptanceId
                && fact.attributes().containsKey("completedAt")) {
            return Optional.of(new EligibleFact(fact.playerId(), "ACH_FIRST_QUEST_COMPLETE",
                    "QUEST_ACCEPTANCE", Long.toString(acceptanceId.longValue()),
                    fact.occurredAt(), null));
        }
        if (event instanceof ItemRewardClaimed fact
                && fact.mailboxEntryId() != null && fact.quantity() > 0) {
            return Optional.of(new EligibleFact(fact.playerId(), "ACH_FIRST_ITEM_CLAIM",
                    "MAILBOX_CLAIM", Long.toString(fact.mailboxEntryId()),
                    fact.occurredAt(), null));
        }
        if (event instanceof QuestRouteCompleted fact) {
            return switch (fact.routeCode()) {
                case "ROUTE_RECORD_START" -> Optional.of(new EligibleFact(
                        fact.playerId(), "ACH_ROUTE_RECORD_START", "ROUTE_COMPLETION",
                        Long.toString(fact.playerRouteId()), fact.occurredAt(), null));
                case "ROUTE_BACKEND_DEVELOPER_START" -> Optional.of(new EligibleFact(
                        fact.playerId(), "ACH_ROUTE_BACKEND_START", "ROUTE_COMPLETION",
                        Long.toString(fact.playerRouteId()), fact.occurredAt(),
                        "TITLE_BACKEND_GUIDE"));
                default -> Optional.empty();
            };
        }
        return Optional.empty();
    }

    record EligibleFact(Long playerId, String achievementCode, String sourceKind,
                        String sourceKey, Instant occurredAt, String linkedTitleCode) {}
}
