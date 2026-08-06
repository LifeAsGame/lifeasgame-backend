package online.lifeasgame.role.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.role.domain.error.RoleError;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleTest {

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
}
