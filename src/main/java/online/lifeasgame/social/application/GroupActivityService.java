package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.social.domain.ActivityGroupType;
import online.lifeasgame.social.domain.GroupActivityDetails;
import online.lifeasgame.social.domain.GroupActivityStatus;
import online.lifeasgame.social.domain.error.SocialError;
import online.lifeasgame.social.infra.GroupActivityStore;
import online.lifeasgame.social.infra.GroupActivityStore.Group;
import online.lifeasgame.social.infra.GroupActivityStore.Member;
import online.lifeasgame.social.infra.GroupActivityStore.Row;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class GroupActivityService {
    private final GroupActivityStore store;
    private final CurrentPlayerAccessor players;
    private final Clock clock;

    public GroupActivityResult.Page<GroupActivityResult.Activity> list(ActivityGroupType type, Long groupId,
                                                                        int page, int size) {
        paging(page, size);
        Long actor = actor();
        Access access = access(type, groupId, actor, false);
        var rows = store.activities(type, groupId, actor, page, size).stream()
                .map(row -> result(type, groupId, row, access)).toList();
        return GroupActivityResult.Page.of(rows, page, size, store.count(type, groupId));
    }

    public GroupActivityResult.Activity detail(ActivityGroupType type, Long groupId, Long activityId) {
        Long actor = actor();
        Access access = access(type, groupId, actor, false);
        return result(type, groupId, activity(type, groupId, activityId, actor), access);
    }

    @Transactional
    public GroupActivityResult.Activity create(ActivityGroupType type, Long groupId, String key, GroupActivityDetails details) {
        if (key == null) throw error(SocialError.GROUP_ACTIVITY_INVALID_INPUT);
        try {
            if (!key.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
                throw error(SocialError.GROUP_ACTIVITY_INVALID_INPUT);
            key = UUID.fromString(key).toString();
        } catch (IllegalArgumentException ex) { throw error(SocialError.GROUP_ACTIVITY_INVALID_INPUT); }
        Long actor = actor();
        Access access = access(type, groupId, actor, true);
        edit(access);
        String hash = hash(details);
        Row previous = store.byKey(type, groupId, actor, key);
        if (previous != null) {
            if (!hash.equals(store.requestHash(previous.id()))) throw error(SocialError.GROUP_ACTIVITY_CONFLICT);
            return result(type, groupId, previous, access);
        }
        long id = store.create(type, groupId, actor, key, hash, details.title(), details.sharedDescription(),
                details.location(), details.startsAt(), details.endsAt(), clock.instant());
        return result(type, groupId, activity(type, groupId, id, actor), access);
    }

    @Transactional
    public GroupActivityResult.Activity update(ActivityGroupType type, Long groupId, Long activityId,
                                                long version, GroupActivityDetails details) {
        Long actor = actor();
        Access access = access(type, groupId, actor, true);
        edit(access);
        Row old = activity(type, groupId, activityId, actor);
        plannedVersion(old, version);
        store.update(activityId, version, details.title(), details.sharedDescription(), details.location(),
                details.startsAt(), details.endsAt(), clock.instant());
        return result(type, groupId, activity(type, groupId, activityId, actor), access);
    }

    @Transactional
    public GroupActivityResult.Activity finish(ActivityGroupType type, Long groupId, Long activityId,
                                                long version, GroupActivityStatus status) {
        if (status == null || status == GroupActivityStatus.PLANNED) throw error(SocialError.GROUP_ACTIVITY_INVALID_INPUT);
        Long actor = actor();
        Access access = access(type, groupId, actor, true);
        edit(access);
        Row old = activity(type, groupId, activityId, actor);
        if (old.version() != version || !old.status().equals("PLANNED") && !old.status().equals(status.name()))
            throw error(SocialError.GROUP_ACTIVITY_CONFLICT);
        if (old.status().equals("PLANNED")) store.finish(activityId, version, status.name(), clock.instant());
        return result(type, groupId, activity(type, groupId, activityId, actor), access);
    }

    @Transactional
    public GroupActivityResult.Activity rsvp(ActivityGroupType type, Long groupId, Long activityId) {
        return changeRsvp(type, groupId, activityId, true);
    }

    @Transactional
    public void withdraw(ActivityGroupType type, Long groupId, Long activityId) {
        changeRsvp(type, groupId, activityId, false);
    }

    private GroupActivityResult.Activity changeRsvp(ActivityGroupType type, Long groupId, Long activityId,
                                                     boolean joining) {
        Long actor = actor();
        Access access = access(type, groupId, actor, true);
        Row row = activity(type, groupId, activityId, actor);
        if (!row.status().equals("PLANNED")) throw error(SocialError.GROUP_ACTIVITY_CONFLICT);
        if (row.myRsvp() != joining) store.rsvp(activityId, actor, access.member(), joining, clock.instant());
        return result(type, groupId, activity(type, groupId, activityId, actor), access);
    }

    public GroupActivityResult.Page<GroupActivityResult.Participant> participants(ActivityGroupType type,
                                                                                    Long groupId, Long activityId,
                                                                                    int page, int size) {
        paging(page, size);
        Long actor = actor();
        access(type, groupId, actor, false);
        Row row = activity(type, groupId, activityId, actor);
        var contents = store.participants(type, activityId, groupId, page, size).stream()
                .map(p -> new GroupActivityResult.Participant(p.playerId(), p.joinedAt())).toList();
        return GroupActivityResult.Page.of(contents, page, size, row.participantCount());
    }

    public GroupActivityResult.Page<GroupActivityResult.Editor> editors(ActivityGroupType type, Long groupId,
                                                                         int page, int size) {
        paging(page, size);
        Access access = access(type, groupId, actor(), false);
        leader(access);
        return GroupActivityResult.Page.of(store.editors(type, groupId, page, size), page, size,
                store.editorCount(type, groupId));
    }

    @Transactional
    public void grant(ActivityGroupType type, Long groupId, Long playerId) {
        Long actor = actor();
        Access access = access(type, groupId, actor, true);
        leader(access);
        Member member = store.member(type, groupId, playerId);
        if (member == null) throw error(SocialError.GROUP_ACTIVITY_NOT_FOUND);
        if (!playerId.equals(access.group().leaderId())) store.grant(type, groupId, playerId, member, clock.instant());
    }

    @Transactional
    public void revoke(ActivityGroupType type, Long groupId, Long playerId) {
        Access access = access(type, groupId, actor(), true);
        leader(access);
        if (playerId.equals(access.group().leaderId())) throw error(SocialError.GROUP_ACTIVITY_CONFLICT);
        store.revoke(type, groupId, playerId);
    }

    private Access access(ActivityGroupType type, Long groupId, Long actor, boolean lock) {
        if (type == null || groupId == null || groupId <= 0) throw error(SocialError.GROUP_ACTIVITY_INVALID_INPUT);
        Group group = store.group(type, groupId, lock);
        if (group == null || !group.status().equals("ACTIVE")) throw error(SocialError.GROUP_ACTIVITY_NOT_FOUND);
        Member member = store.member(type, groupId, actor);
        if (member == null) throw error(SocialError.GROUP_ACTIVITY_NOT_FOUND);
        boolean leader = actor.equals(group.leaderId());
        return new Access(group, member, leader, leader || store.editor(type, groupId, actor, member));
    }

    private Row activity(ActivityGroupType type, Long groupId, Long activityId, Long actor) {
        Row row = store.activity(type, groupId, activityId, actor);
        if (row == null) throw error(SocialError.GROUP_ACTIVITY_NOT_FOUND);
        return row;
    }

    private static GroupActivityResult.Activity result(ActivityGroupType type, Long groupId, Row row, Access access) {
        boolean planned = row.status().equals("PLANNED");
        return new GroupActivityResult.Activity(row.id(), type.name(), groupId, row.title(), row.description(),
                row.location(), row.startsAt(), row.endsAt(), row.status(), row.creatorId(), row.createdAt(),
                row.updatedAt(), row.version(), row.participantCount(), row.myRsvp(),
                new GroupActivityResult.Capabilities(access.canEdit() && planned, access.leader(), planned));
    }

    private static void plannedVersion(Row row, long version) {
        if (version < 0 || row.version() != version || !row.status().equals("PLANNED"))
            throw error(SocialError.GROUP_ACTIVITY_CONFLICT);
    }
    private static void edit(Access access) {
        if (!access.canEdit()) throw error(SocialError.GROUP_ACTIVITY_FORBIDDEN);
    }
    private static void leader(Access access) {
        if (!access.leader()) throw error(SocialError.GROUP_ACTIVITY_FORBIDDEN);
    }
    private static void paging(int page, int size) {
        if (page < 0 || page > 1000 || size < 1 || size > 50) throw error(SocialError.GROUP_ACTIVITY_INVALID_INPUT);
    }
    private static String hash(GroupActivityDetails details) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(String.join("\u0000",
                    details.title(), hashPart(details.sharedDescription()), hashPart(details.location()),
                    details.startsAt().toString(), details.endsAt().toString()).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private static String hashPart(String value) { return value == null ? "-1:" : value.length() + ":" + value; }
    private Long actor() { return players.currentPlayerIdOrThrow(); }
    private static DomainException error(SocialError code) { return new DomainException(code); }
    private record Access(Group group, Member member, boolean leader, boolean canEdit) {}
}
