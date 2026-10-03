package online.lifeasgame.quest.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.lifelog.application.internal.LifeLogEvidenceReadApi;
import online.lifeasgame.quest.application.result.QuestResult;
import online.lifeasgame.quest.domain.JourneyEvidence;
import online.lifeasgame.quest.domain.Quest;
import online.lifeasgame.quest.domain.QuestAcceptance;
import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.quest.domain.error.QuestError;
import online.lifeasgame.quest.domain.repository.JourneyEvidenceStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Clock;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BackendJourneyEvidenceService {
    private final QuestReader quests;
    private final QuestAcceptanceCompletionService completion;
    private final JourneyEvidenceStore evidenceStore;
    private final BackendJourneyAccess access;
    private final LifeLogEvidenceReadApi lifeLogs;
    private final CurrentPlayerAccessor currentPlayer;
    private final Clock clock;

    @Transactional
    public QuestResult.Acceptance linkMemo(String code, String memo) {
        QuestCode questCode = parse(code);
        if (questCode != QuestCode.Q_DEV_DEFINE_BACKEND_GOAL) throw invalid();
        return link(questCode, "GOAL_MEMO", null, text(memo), null, null);
    }

    @Transactional
    public QuestResult.Acceptance linkLifeLog(String code, Long lifeLogId) {
        QuestCode questCode = parse(code);
        boolean java = questCode == QuestCode.Q_DEV_RECORD_JAVA_STUDY;
        boolean project = switch (questCode) {
            case Q_DEV_BUILD_SPRING_CRUD, Q_DEV_MODEL_DATABASE,
                    Q_DEV_WRITE_DOMAIN_TEST, Q_DEV_POLISH_README -> true;
            default -> false;
        };
        if (!java && !project || lifeLogId == null || lifeLogId <= 0) throw invalid();
        var owned = ownedAcceptance(questCode, true);
        String kind = project ? "PROJECT" : "LIFE_LOG";
        var previous = evidenceStore.find(owned.acceptance().getId());
        if (previous.isPresent() && previous.get().kind().equals(kind)
                && lifeLogId.equals(previous.get().lifeLogId())) {
            return QuestResult.Acceptance.from(owned.acceptance(), owned.quest());
        }
        Long playerId = currentPlayer.currentPlayerIdOrThrow();
        Long roleId = access.requireSelectedRole(playerId);
        var source = lifeLogs.requireContentfulOwned(playerId, lifeLogId);
        if (project && !source.projectCollection()) throw invalid();
        if (source.primaryRoleId() != null && !source.primaryRoleId().equals(roleId)) {
            throw new DomainException(QuestError.JOURNEY_ROLE_MISMATCH);
        }
        return link(questCode, kind, lifeLogId, null, null, null);
    }

    @Transactional
    public QuestResult.Acceptance linkDeployment(String code, String url, String description) {
        QuestCode questCode = parse(code);
        if (questCode != QuestCode.Q_DEV_DEPLOY_SERVICE) throw invalid();
        String normalizedUrl = validUrl(url);
        return link(questCode, "DEPLOYMENT", null, null, normalizedUrl, text(description));
    }

    @Transactional(readOnly = true)
    public JourneyEvidence evidence(String code) {
        QuestCode questCode = parse(code);
        var owned = ownedAcceptance(questCode, false);
        return evidenceStore.find(owned.acceptance().getId()).orElse(null);
    }

    @Transactional
    public QuestResult.Acceptance unlink(String code) {
        QuestCode questCode = parse(code);
        var owned = ownedAcceptance(questCode, true);
        QuestAcceptance acceptance = owned.acceptance();
        if (acceptance.isCompleted() || acceptance.isCanceled()) throw conflict();
        if (evidenceStore.find(acceptance.getId()).isPresent()) {
            evidenceStore.delete(acceptance.getId());
            acceptance.clearJourneyEvidence();
        }
        return QuestResult.Acceptance.from(acceptance, owned.quest());
    }

    @Transactional
    public QuestResult.Acceptance complete(String code) {
        QuestCode questCode = parse(code);
        var owned = ownedAcceptance(questCode, true);
        QuestAcceptance acceptance = owned.acceptance();
        if (acceptance.isCompleted()) return QuestResult.Acceptance.from(acceptance, owned.quest());
        JourneyEvidence evidence = evidenceStore.find(acceptance.getId())
                .orElseThrow(BackendJourneyEvidenceService::conflict);
        if (!acceptance.isGoalReached()) throw conflict();
        if (evidence.lifeLogId() != null) {
            Long playerId = currentPlayer.currentPlayerIdOrThrow();
            Long roleId = access.requireSelectedRole(playerId);
            var source = lifeLogs.requireContentfulOwned(playerId, evidence.lifeLogId());
            if ("PROJECT".equals(evidence.kind()) && !source.projectCollection()) throw invalid();
            if (source.primaryRoleId() != null && !source.primaryRoleId().equals(roleId)) {
                throw new DomainException(QuestError.JOURNEY_ROLE_MISMATCH);
            }
        }
        return completion.completeForPlayer(acceptance.getPlayerId(), acceptance.getId());
    }

    private QuestResult.Acceptance link(
            QuestCode questCode, String kind, Long lifeLogId, String memo,
            String url, String description
    ) {
        var owned = ownedAcceptance(questCode, true);
        QuestAcceptance acceptance = owned.acceptance();
        JourneyEvidence next = new JourneyEvidence(
                acceptance.getId(), kind, lifeLogId, memo, url, description, clock.instant());
        var existing = evidenceStore.find(acceptance.getId());
        if (existing.isPresent()) {
            if (!existing.get().sameContent(next)) throw conflict();
            return QuestResult.Acceptance.from(acceptance, owned.quest());
        }
        if (!acceptance.isInProgress()) throw conflict();
        evidenceStore.insert(next);
        acceptance.setProgress(1, owned.quest(), clock.instant());
        return QuestResult.Acceptance.from(acceptance, owned.quest());
    }

    private Owned ownedAcceptance(QuestCode code, boolean forUpdate) {
        Long playerId = currentPlayer.currentPlayerIdOrThrow();
        Quest quest = quests.getByCode(code);
        QuestAcceptance latest = quests.findLatest(quest.getId(), playerId);
        if (latest == null) throw new DomainException(QuestError.QUEST_ACCEPTANCE_NOT_FOUND);
        QuestAcceptance acceptance = forUpdate ? quests.getAcceptanceForUpdate(latest.getId()) : latest;
        if (!Objects.equals(playerId, acceptance.getPlayerId())) {
            throw new DomainException(QuestError.QUEST_ACCEPTANCE_NOT_FOUND);
        }
        if (!acceptance.isCompleted()) access.requireSelectedRole(playerId);
        return new Owned(quest, acceptance);
    }

    private static QuestCode parse(String raw) {
        QuestCode code = QuestCode.parse(raw);
        if (!BackendJourneyAccess.isJourney(code)) throw invalid();
        return code;
    }

    private static String text(String raw) {
        if (raw == null) throw invalid();
        String value = raw.strip();
        if (value.isEmpty() || value.length() > 1000) throw invalid();
        return value;
    }

    private static String validUrl(String raw) {
        if (raw == null || raw.length() > 2048) throw invalid();
        String value = raw.strip();
        try {
            URI uri = URI.create(value);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getHost().isBlank()
                    || uri.getRawUserInfo() != null || uri.getPort() > 65535) throw invalid();
            return value;
        } catch (IllegalArgumentException exception) {
            throw invalid();
        }
    }

    private static DomainException invalid() {
        return new DomainException(QuestError.JOURNEY_EVIDENCE_INVALID);
    }

    private static DomainException conflict() {
        return new DomainException(QuestError.JOURNEY_EVIDENCE_CONFLICT);
    }

    private record Owned(Quest quest, QuestAcceptance acceptance) {
    }
}
