package online.lifeasgame.role.application.result;

import online.lifeasgame.person.application.internal.PersonLookupApi.PersonReference;
import online.lifeasgame.role.domain.RoleRelation;

import java.time.Instant;

public final class RoleRelationResult {

    private RoleRelationResult() {
    }

    public record Stored(
            Long id,
            Long playerId,
            Long roleId,
            Long personId,
            String relationType,
            String roleNotes,
            String status,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
        public static Stored from(RoleRelation relation) {
            return new Stored(
                    relation.getId(),
                    relation.getPlayerId(),
                    relation.getRoleId(),
                    relation.getPersonId(),
                    relation.getRelationType().value(),
                    relation.getRoleNotes(),
                    relation.getStatus().name(),
                    relation.getCreatedAt(),
                    relation.getUpdatedAt(),
                    relation.getVersion()
            );
        }
    }

    public record Detail(
            Long id,
            Long playerId,
            Long roleId,
            Long personId,
            String personDisplayName,
            Long linkedUserId,
            String relationType,
            String roleNotes,
            String status,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
        public static Detail from(RoleRelation relation, PersonReference person) {
            return from(Stored.from(relation), person);
        }

        public static Detail from(Stored stored, PersonReference person) {
            return new Detail(
                    stored.id(),
                    stored.playerId(),
                    stored.roleId(),
                    stored.personId(),
                    person.displayName(),
                    person.linkedUserId(),
                    stored.relationType(),
                    stored.roleNotes(),
                    stored.status(),
                    stored.createdAt(),
                    stored.updatedAt(),
                    stored.version()
            );
        }
    }
}
