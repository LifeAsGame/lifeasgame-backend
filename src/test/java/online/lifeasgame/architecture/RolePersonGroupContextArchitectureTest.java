package online.lifeasgame.architecture;

import online.lifeasgame.person.application.internal.PersonLookupApi;
import online.lifeasgame.role.application.PersonRoleContextFinder;
import online.lifeasgame.role.application.internal.RoleLookupApi;
import online.lifeasgame.social.application.PersonalRoleGroupFinder;
import online.lifeasgame.social.application.PersonalRoleGroupLinker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class RolePersonGroupContextArchitectureTest {
    @Test
    @DisplayName("role→person, social→role 공개 포트로 조회·연결하고 역방향 의존을 만들지 않는다")
    void keepsProviderOwnedBoundaries() {
        assertForeignBoundary(PersonRoleContextFinder.class, "role", PersonLookupApi.class);
        assertForeignBoundary(PersonalRoleGroupFinder.class, "social", RoleLookupApi.class);
        assertForeignBoundary(PersonalRoleGroupLinker.class, "social", RoleLookupApi.class);
    }

    private static void assertForeignBoundary(Class<?> type, String ownModule, Class<?> port) {
        var foreignDependencies = Arrays.stream(type.getDeclaredFields()).map(field -> field.getType())
                .filter(dependency -> dependency.getPackageName().startsWith("online.lifeasgame."))
                .filter(dependency -> !dependency.getPackageName().startsWith("online.lifeasgame." + ownModule + "."))
                .filter(dependency -> !dependency.getPackageName().startsWith("online.lifeasgame.core."))
                .toList();
        assertThat(foreignDependencies.stream().map(Class::getName).toList()).containsExactly(port.getName());
    }
}
