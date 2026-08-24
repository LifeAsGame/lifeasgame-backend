package online.lifeasgame.user.application.internal;

import java.util.Optional;

public interface UserAuthApi {

    Long authenticate(String email, String rawPassword);

    Long register(String email, String rawPassword, String nickname);

    Long findOrRegisterByGoogle(String email, String name);

    Optional<AccountAuthorization> resolveAuthorization(Long userId);

    record AccountAuthorization(boolean active, boolean admin) {
    }
}
