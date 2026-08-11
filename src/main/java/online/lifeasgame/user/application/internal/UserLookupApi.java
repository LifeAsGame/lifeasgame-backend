package online.lifeasgame.user.application.internal;

public interface UserLookupApi {

    UserReference getActive(Long userId);

    record UserReference(Long id, String nickname) {
    }
}
