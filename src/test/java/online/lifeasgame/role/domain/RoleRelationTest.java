package online.lifeasgame.role.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.role.domain.error.RoleError;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleRelationTest {

    @Test
    void createsUpdatesArchivesAndReactivates() {
        RoleRelation relation = relation("  family  ", " note ");

        assertThat(relation.getRelationType().value()).isEqualTo("FAMILY");
        assertThat(relation.getRoleNotes()).isEqualTo("note");
        assertThat(relation.getStatus()).isEqualTo(RoleRelationStatus.ACTIVE);

        relation.update(RoleRelationType.of("friend"), " updated ");
        assertThat(relation.getRelationType().value()).isEqualTo("FRIEND");
        assertThat(relation.getRoleNotes()).isEqualTo("updated");

        relation.archive();
        relation.archive();
        assertThat(relation.getStatus()).isEqualTo(RoleRelationStatus.ARCHIVED);
        assertThatThrownBy(() -> relation.update(
                RoleRelationType.of("WORK"),
                null
        )).isInstanceOfSatisfying(DomainException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(RoleError.ROLE_RELATION_ARCHIVED)
        );

        relation.reactivate(RoleRelationType.of("work"), "   ");
        assertThat(relation.getRelationType().value()).isEqualTo("WORK");
        assertThat(relation.getRoleNotes()).isNull();
        assertThat(relation.getStatus()).isEqualTo(RoleRelationStatus.ACTIVE);
    }

    @Test
    void rejectsInvalidRelationType() {
        assertInvalidType(null);
        assertInvalidType("   ");
        assertInvalidType("x".repeat(41));
    }

    @Test
    void acceptsTrimmedFortyCharacterOpenCode() {
        RoleRelation relation = relation("  " + "x".repeat(40) + "  ", null);

        assertThat(relation.getRelationType().value()).isEqualTo("X".repeat(40));
    }

    private RoleRelation relation(String relationType, String roleNotes) {
        return RoleRelation.create(
                1L,
                2L,
                3L,
                RoleRelationType.of(relationType),
                roleNotes
        );
    }

    private void assertInvalidType(String value) {
        assertThatThrownBy(() -> RoleRelationType.of(value))
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(RoleError.INVALID_ROLE_RELATION_TYPE)
                );
    }
}
