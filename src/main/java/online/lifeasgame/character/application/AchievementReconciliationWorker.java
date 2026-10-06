package online.lifeasgame.character.application;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.platform.outbox.application.OutboxPlayerFactReadApi;
import online.lifeasgame.quest.application.internal.QuestAchievementEvidenceApi;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AchievementReconciliationWorker {
    private final OutboxPlayerFactReadApi outboxFacts;
    private final QuestAchievementEvidenceApi questEvidence;
    private final AchievementFactProcessor processor;
    private final JdbcTemplate jdbc;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PlayerResult process(Long playerId, boolean apply) {
        Map<String, Candidate> candidates = findEvidence(playerId);
        int granted = 0;
        int existing = 0;
        int revoked = 0;
        int unknown = 0;
        for (String code : new String[] {"ACH_FIRST_LIFELOG", "ACH_FIRST_QUEST_COMPLETE",
                "ACH_FIRST_ITEM_CLAIM", "ACH_ROUTE_RECORD_START", "ACH_ROUTE_BACKEND_START"}) {
            String receipt = jdbc.query("""
                    SELECT status FROM achievement_award_receipts
                    WHERE player_id = ? AND achievement_code = ?
                    """, rs -> rs.next() ? rs.getString(1) : null, playerId, code);
            if ("REVOKED".equals(receipt)) {
                revoked++;
                continue;
            }
            if (receipt != null) {
                existing++;
                continue;
            }
            Candidate candidate = candidates.get(code);
            if (candidate == null) {
                unknown++;
                continue;
            }
            Integer owned = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM player_achievements pa
                    JOIN achievements a ON a.id = pa.achievement_id
                    WHERE pa.player_id = ? AND a.code = ?
                    """, Integer.class, playerId, code);
            if (owned != null && owned > 0) {
                existing++;
            } else {
                granted++;
            }
            if (apply) processor.award(candidate.fact, candidate.eventId, candidate.provenance);
        }
        return new PlayerResult(playerId, granted, existing, revoked, unknown);
    }

    private Map<String, Candidate> findEvidence(Long playerId) {
        Map<String, Candidate> candidates = new HashMap<>();
        for (var delivery : outboxFacts.retainedFacts(playerId)) {
            AchievementActivationRule.match(delivery.event()).ifPresent(fact ->
                    addEarlier(candidates, fact.achievementCode(),
                            new Candidate(fact, delivery.eventId(), "DURABLE_EVENT")));
        }
        if (!candidates.containsKey("ACH_FIRST_QUEST_COMPLETE")) {
            questEvidence.completedAcceptances(playerId).stream().findFirst().ifPresent(state ->
                    addEarlier(candidates, "ACH_FIRST_QUEST_COMPLETE", new Candidate(
                            new AchievementActivationRule.EligibleFact(playerId,
                                    "ACH_FIRST_QUEST_COMPLETE", "QUEST_ACCEPTANCE",
                                    Long.toString(state.id()), state.completedAt(), null),
                            null, "CONFIRMED_STATE")));
        }
        for (var state : questEvidence.completedRoutes(playerId)) {
            String code = state.routeCode().equals("ROUTE_RECORD_START")
                    ? "ACH_ROUTE_RECORD_START" : "ACH_ROUTE_BACKEND_START";
            String title = code.equals("ACH_ROUTE_BACKEND_START") ? "TITLE_BACKEND_GUIDE" : null;
            addEarlier(candidates, code, new Candidate(
                    new AchievementActivationRule.EligibleFact(playerId, code,
                            "ROUTE_COMPLETION", Long.toString(state.id()), state.completedAt(), title),
                    null, "CONFIRMED_STATE"));
        }
        return candidates;
    }

    private void addEarlier(Map<String, Candidate> candidates, String code, Candidate candidate) {
        candidates.merge(code, candidate,
                (oldValue, newValue) -> Comparator.comparing(
                        (Candidate value) -> value.fact.occurredAt())
                        .compare(oldValue, newValue) <= 0 ? oldValue : newValue);
    }

    private record Candidate(AchievementActivationRule.EligibleFact fact,
                             String eventId, String provenance) {}
    public record PlayerResult(Long playerId, int grantable, int existing,
                               int revoked, int unknown) {}
}
