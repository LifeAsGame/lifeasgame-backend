package online.lifeasgame.demo.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.demo.domain.DemoError;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DemoRunStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final DemoProperties properties;

    public record Proof(String id, String cookie, String header, Instant expiresAt) {}
    public record Run(String id, String status, String failureCode, Instant createdAt, Instant expiresAt) {}
    public record Actor(String runId, String name, Long userId, Long playerId) {}

    public static byte[] hash(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant instant(LocalDateTime value) {
        return value.toInstant(ZoneOffset.UTC);
    }

    public Proof issueProof(String cookie, String header) {
        jdbc.update("DELETE FROM portfolio_demo_proofs WHERE expires_at<?",
                utc(Instant.now().minusSeconds(86400)));
        int outstanding = jdbc.queryForObject(
                "SELECT COUNT(*) FROM portfolio_demo_proofs WHERE expires_at>?",
                Integer.class, utc(Instant.now()));
        if (outstanding >= 1000) throw new DomainException(DemoError.CAPACITY_EXCEEDED);
        Instant expiresAt = Instant.now().plusSeconds(900);
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO portfolio_demo_proofs (id,cookie_hash,header_hash,expires_at) VALUES (?,?,?,?)",
                id, hash(cookie), hash(header), utc(expiresAt));
        return new Proof(id, cookie, header, expiresAt);
    }

    public String proofId(String cookie, String header) {
        if (cookie == null || header == null) throw new DomainException(DemoError.PROOF_INVALID);
        List<String> ids = jdbc.query("SELECT id FROM portfolio_demo_proofs WHERE cookie_hash=? AND header_hash=? AND expires_at>?",
                (rs, row) -> rs.getString(1), hash(cookie), hash(header), utc(Instant.now()));
        if (ids.size() != 1) throw new DomainException(DemoError.PROOF_INVALID);
        return ids.getFirst();
    }

    public Run start(String proofId, String attemptKey, String managerToken) {
        if (attemptKey == null || attemptKey.length() > 80 || !attemptKey.matches("[A-Za-z0-9_-]{16,80}")) {
            throw new DomainException(DemoError.INVALID_REQUEST);
        }
        return transactions.execute(status -> {
            jdbc.queryForObject("SELECT id FROM portfolio_demo_capacity WHERE id=1 FOR UPDATE", Integer.class);
            Integer validProof = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM portfolio_demo_proofs WHERE id=? AND expires_at>?",
                    Integer.class, proofId, utc(Instant.now()));
            if (validProof == null || validProof != 1) throw new DomainException(DemoError.PROOF_INVALID);
            List<Run> previous = jdbc.query(
                    "SELECT id,status,failure_code,created_at,expires_at FROM portfolio_demo_runs WHERE proof_id=? AND attempt_key=?",
                    (rs, row) -> mapRun(rs), proofId, attemptKey);
            if (!previous.isEmpty()) return previous.getFirst();
            LocalDate day = LocalDate.now(ZoneOffset.UTC);
            LocalDate savedDay = jdbc.queryForObject(
                    "SELECT last_creation_day FROM portfolio_demo_capacity WHERE id=1", LocalDate.class);
            int count = jdbc.queryForObject(
                    "SELECT creations_today FROM portfolio_demo_capacity WHERE id=1", Integer.class);
            if (!day.equals(savedDay)) count = 0;
            int active = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM portfolio_demo_runs WHERE status IN ('PROVISIONING','READY','FAILED') AND expires_at>?",
                    Integer.class, utc(Instant.now()));
            int total = jdbc.queryForObject("SELECT COUNT(*) FROM portfolio_demo_runs", Integer.class);
            if (active >= properties.maxActiveRuns() || count >= properties.maxDailyRuns() ||
                    total >= properties.maxTotalRuns()) {
                throw new DomainException(DemoError.CAPACITY_EXCEEDED);
            }
            jdbc.update("UPDATE portfolio_demo_capacity SET last_creation_day=?,creations_today=? WHERE id=1", day, count + 1);
            Instant now = Instant.now();
            Run run = new Run(UUID.randomUUID().toString(), "PROVISIONING", null, now,
                    now.plusSeconds(3600L * properties.ttlHours()));
            jdbc.update("INSERT INTO portfolio_demo_runs (id,proof_id,attempt_key,manager_hash,status,created_at,expires_at) VALUES (?,?,?,?,?,?,?)",
                    run.id(), proofId, attemptKey, hash(managerToken), run.status(), utc(run.createdAt()), utc(run.expiresAt()));
            return run;
        });
    }

    private static Run mapRun(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Run(rs.getString(1), rs.getString(2), rs.getString(3),
                instant(rs.getObject(4, LocalDateTime.class)), instant(rs.getObject(5, LocalDateTime.class)));
    }

    public Run byManager(String manager) {
        if (manager == null) throw new DomainException(DemoError.SESSION_INVALID);
        List<Run> runs = jdbc.query(
                "SELECT id,status,failure_code,created_at,expires_at FROM portfolio_demo_runs WHERE manager_hash=?",
                (rs, row) -> mapRun(rs), hash(manager));
        if (runs.size() != 1) throw new DomainException(DemoError.SESSION_INVALID);
        return effective(runs.getFirst());
    }

    public Run byId(String id) {
        List<Run> runs = jdbc.query(
                "SELECT id,status,failure_code,created_at,expires_at FROM portfolio_demo_runs WHERE id=?",
                (rs, row) -> mapRun(rs), id);
        if (runs.size() != 1) throw new DomainException(DemoError.RUN_NOT_FOUND);
        return effective(runs.getFirst());
    }

    private Run effective(Run run) {
        if (run.expiresAt().isBefore(Instant.now()) && !run.status().equals("CLOSED") && !run.status().equals("EXPIRED")) {
            jdbc.update("UPDATE portfolio_demo_runs SET status='EXPIRED' WHERE id=? AND status NOT IN ('CLOSED','EXPIRED')", run.id());
            return new Run(run.id(), "EXPIRED", null, run.createdAt(), run.expiresAt());
        }
        return run;
    }

    public Actor actor(String runId, String name) {
        List<Actor> actors = jdbc.query("SELECT run_id,actor,user_id,player_id FROM portfolio_demo_actors WHERE run_id=? AND actor=?",
                (rs, row) -> new Actor(rs.getString(1), rs.getString(2), rs.getLong(3), (Long) rs.getObject(4)),
                runId, name);
        if (actors.size() != 1 || actors.getFirst().playerId() == null) throw new DomainException(DemoError.NOT_READY);
        return actors.getFirst();
    }

    public Actor actorByUser(Long userId) {
        List<Actor> actors = jdbc.query("SELECT run_id,actor,user_id,player_id FROM portfolio_demo_actors WHERE user_id=?",
                (rs, row) -> new Actor(rs.getString(1), rs.getString(2), rs.getLong(3), (Long) rs.getObject(4)), userId);
        return actors.isEmpty() ? null : actors.getFirst();
    }

    public String runIdByPlayer(Long playerId) {
        List<String> ids = jdbc.query("SELECT run_id FROM portfolio_demo_actors WHERE player_id=?",
                (rs, row) -> rs.getString(1), playerId);
        return ids.isEmpty() ? null : ids.getFirst();
    }

    public boolean sameBoundary(Long leftPlayer, Long rightPlayer) {
        String left = runIdByPlayer(leftPlayer);
        String right = runIdByPlayer(rightPlayer);
        return left == null ? right == null : left.equals(right);
    }

    public void bindActor(String runId, String name, Long userId, Long playerId) {
        jdbc.update("INSERT INTO portfolio_demo_actors (run_id,actor,user_id,player_id) VALUES (?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE player_id=VALUES(player_id)",
                runId, name, userId, playerId);
    }

    public void markReady(String runId) {
        jdbc.update("UPDATE portfolio_demo_runs SET status='READY',failure_code=NULL,lease_until=NULL WHERE id=? AND status='PROVISIONING'", runId);
    }

    public void markFailed(String runId) {
        jdbc.update("UPDATE portfolio_demo_runs SET status='FAILED',failure_code='DEMO-PROVISION-FAILED',lease_until=NULL WHERE id=? AND status='PROVISIONING'", runId);
    }

    public boolean retry(String runId) {
        return jdbc.update("UPDATE portfolio_demo_runs SET status='PROVISIONING',failure_code=NULL,lease_until=NULL WHERE id=? AND status='FAILED' AND expires_at>?",
                runId, utc(Instant.now())) == 1;
    }

    public boolean claimProvisioning(String runId) {
        return jdbc.update("UPDATE portfolio_demo_runs SET lease_until=? WHERE id=? AND status='PROVISIONING' AND expires_at>? " +
                        "AND (lease_until IS NULL OR lease_until<?)",
                utc(Instant.now().plusSeconds(600)), runId, utc(Instant.now()), utc(Instant.now())) == 1;
    }

    public record Targets(Long listingId, Long roleId, Long projectLifeLogId, Long channelId) {}

    public void saveTargets(String runId, Targets targets) {
        jdbc.update("INSERT INTO portfolio_demo_targets (run_id,listing_id,role_id,project_life_log_id,channel_id) " +
                        "VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE listing_id=VALUES(listing_id),role_id=VALUES(role_id)," +
                        "project_life_log_id=VALUES(project_life_log_id),channel_id=VALUES(channel_id)",
                runId, targets.listingId(), targets.roleId(), targets.projectLifeLogId(), targets.channelId());
    }

    public Targets targets(String runId) {
        List<Targets> values = jdbc.query(
                "SELECT listing_id,role_id,project_life_log_id,channel_id FROM portfolio_demo_targets WHERE run_id=?",
                (rs, row) -> new Targets((Long) rs.getObject(1), (Long) rs.getObject(2),
                        (Long) rs.getObject(3), (Long) rs.getObject(4)), runId);
        return values.isEmpty() ? null : values.getFirst();
    }

    public void close(String runId) {
        jdbc.update("UPDATE portfolio_demo_runs SET status='CLOSED' WHERE id=? AND status IN ('PROVISIONING','READY','FAILED')", runId);
    }

    public String issuePeerLink(String runId, String code) {
        return transactions.execute(status -> {
            jdbc.queryForObject("SELECT id FROM portfolio_demo_runs WHERE id=? FOR UPDATE", String.class, runId);
            jdbc.update("DELETE FROM portfolio_demo_peer_links WHERE run_id=?", runId);
            jdbc.update("INSERT INTO portfolio_demo_peer_links (code_hash,run_id,expires_at) VALUES (?,?,?)",
                    hash(code), runId, utc(Instant.now().plusSeconds(300)));
            return code;
        });
    }

    public String redeemPeerLink(String code) {
        if (code == null || code.isBlank()) throw new DomainException(DemoError.INVALID_REQUEST);
        return transactions.execute(status -> {
            List<String> ids = jdbc.query(
                    "SELECT run_id FROM portfolio_demo_peer_links WHERE code_hash=? AND redeemed_at IS NULL AND expires_at>? FOR UPDATE",
                    (rs, row) -> rs.getString(1), hash(code), utc(Instant.now()));
            if (ids.size() != 1) throw new DomainException(DemoError.SESSION_INVALID);
            Run run = byId(ids.getFirst());
            if (!run.status().equals("READY")) throw new DomainException(DemoError.NOT_READY);
            jdbc.update("UPDATE portfolio_demo_peer_links SET redeemed_at=? WHERE code_hash=?",
                    utc(Instant.now()), hash(code));
            return run.id();
        });
    }
}
