package online.lifeasgame.quest.domain.repository;

import online.lifeasgame.quest.domain.JourneyEvidence;
import java.util.Optional;

public interface JourneyEvidenceStore {
    Optional<JourneyEvidence> find(Long acceptanceId);
    void insert(JourneyEvidence evidence);
    void delete(Long acceptanceId);
}
