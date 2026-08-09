package online.lifeasgame.architecture;

import online.lifeasgame.auth.application.AuthFacade;
import online.lifeasgame.auth.application.AuthService;
import online.lifeasgame.auth.application.internal.AuthTokenApi;
import online.lifeasgame.character.application.PlayerFacade;
import online.lifeasgame.character.application.PlayerLookupService;
import online.lifeasgame.character.application.PlayerService;
import online.lifeasgame.character.application.internal.PlayerLookupApi;
import online.lifeasgame.user.application.UserAuthService;
import online.lifeasgame.user.application.UserService;
import online.lifeasgame.user.application.internal.UserAuthApi;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserAuthPlayerArchitectureTest {

    @Test
    void authFacadeDependsOnProviderOwnedApisInsteadOfConcreteContextServices() {
        Set<Class<?>> dependencies = fieldTypes(AuthFacade.class);

        assertThat(dependencies)
                .contains(UserAuthApi.class, PlayerLookupApi.class, AuthService.class)
                .doesNotContain(UserService.class, PlayerService.class);
    }

    @Test
    void playerOnboardingDependsOnAuthTokenContractInsteadOfAuthService() {
        assertThat(fieldTypes(PlayerFacade.class))
                .contains(AuthTokenApi.class)
                .doesNotContain(AuthService.class);
        assertThat(AuthTokenApi.class.isAssignableFrom(AuthService.class)).isTrue();
    }

    @Test
    void providerOwnedApisExposeNoEntityRepositoryOrInfrastructureTypes() {
        assertBoundaryTypes(UserAuthApi.class);
        assertBoundaryTypes(PlayerLookupApi.class);
        assertBoundaryTypes(AuthTokenApi.class);
        assertThat(UserAuthApi.class.isAssignableFrom(UserAuthService.class)).isTrue();
        assertThat(PlayerLookupApi.class.isAssignableFrom(PlayerLookupService.class)).isTrue();
    }

    @Test
    void simpleFacadesAreRemovedAndPlayerFacadeKeepsOnlyOnboarding() {
        for (String className : Set.of(
                "online.lifeasgame.user.application.UserFacade",
                "online.lifeasgame.user.application.UserSettingFacade",
                "online.lifeasgame.character.application.PlayerAchievementFacade",
                "online.lifeasgame.character.application.PlayerCertificationFacade",
                "online.lifeasgame.character.application.PlayerEquipmentFacade",
                "online.lifeasgame.character.application.PlayerHobbyFacade",
                "online.lifeasgame.character.application.PlayerTitleFacade"
        )) {
            assertThatThrownBy(() -> Class.forName(className))
                    .isInstanceOf(ClassNotFoundException.class);
        }

        assertThat(Arrays.stream(PlayerFacade.class.getDeclaredMethods())
                .map(Method::getName))
                .containsExactly("linkStart");
    }

    private static Set<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .map(field -> field.getType())
                .collect(java.util.stream.Collectors.toSet());
    }

    private static void assertBoundaryTypes(Class<?> api) {
        Arrays.stream(api.getDeclaredMethods())
                .flatMap(method -> java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(method.getReturnType()),
                        Arrays.stream(method.getParameterTypes())
                ))
                .map(Class::getPackageName)
                .forEach(packageName -> assertThat(packageName)
                        .doesNotContain(".domain")
                        .doesNotContain(".repository")
                        .doesNotContain(".infra"));
    }
}
