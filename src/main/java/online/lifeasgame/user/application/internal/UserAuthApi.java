package online.lifeasgame.user.application.internal;

public interface UserAuthApi {

    Long authenticate(String email, String rawPassword);

    Long register(String email, String rawPassword, String nickname);

    Long findOrRegisterByGoogle(String email, String name);
}
