package online.lifeasgame.role.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.role.domain.error.RoleError;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleTest {

    @Test
    void rejectsInvalidRoleTypeWithDomainError() {
        assertRoleError(() -> RoleType.of(null), RoleError.INVALID_ROLE_TYPE);
        assertRoleError(() -> RoleType.of("   "), RoleError.INVALID_ROLE_TYPE);
        assertRoleError(() -> RoleType.of("x".repeat(41)), RoleError.INVALID_ROLE_TYPE);
    }

    @Test
    void rejectsInvalidNameAndDescriptionWithDomainErrors() {
        assertRoleError(
                () -> Role.create(1L, RoleType.of("SELF"), null, null),
                RoleError.INVALID_ROLE_NAME
        );
        assertRoleError(
                () -> Role.create(1L, RoleType.of("SELF"), "   ", null),
                RoleError.INVALID_ROLE_NAME
        );
        assertRoleError(
                () -> Role.create(1L, RoleType.of("SELF"), "x".repeat(61), null),
                RoleError.INVALID_ROLE_NAME
        );
        assertRoleError(
                () -> Role.create(1L, RoleType.of("SELF"), "Self", "x".repeat(501)),
                RoleError.INVALID_ROLE_DESCRIPTION
        );
    }

    @Test
    void validatesLengthsAfterTrimming() {
        Role role = Role.create(
                1L,
                RoleType.of("  " + "x".repeat(40) + "  "),
                "  " + "n".repeat(60) + "  ",
                "  " + "d".repeat(500) + "  "
        );

        assertThat(role.getRoleType().value()).isEqualTo("X".repeat(40));
        assertThat(role.getName()).hasSize(60);
        assertThat(role.getDescription()).hasSize(500);
    }

    @Test
    void normalizesAndFullyUpdatesRole() {
        Role role = Role.create(1L, RoleType.of("  family  "), "  Parent  ", " note ");

        role.update(RoleType.of("work"), "Developer", " builds things ");

        assertThat(role.getPlayerId()).isEqualTo(1L);
        assertThat(role.getRoleType().value()).isEqualTo("WORK");
        assertThat(role.getName()).isEqualTo("Developer");
        assertThat(role.getDescription()).isEqualTo("builds things");
        assertThat(role.getStatus()).isEqualTo(RoleStatus.ACTIVE);
    }

    @Test
    void archiveIsIdempotentAndRejectsFurtherUpdates() {
        Role role = Role.create(1L, RoleType.of("SELF"), "Self", null);

        role.archive();
        role.archive();

        assertThat(role.getStatus()).isEqualTo(RoleStatus.ARCHIVED);
        assertThatThrownBy(() -> role.update(RoleType.of("WORK"), "Work", null))
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(RoleError.ROLE_ARCHIVED)
                );
    }

    private void assertRoleError(Runnable action, RoleError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(error)
                );
    }
}
