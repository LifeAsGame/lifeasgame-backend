package online.lifeasgame.auth.application.result;

public final class AuthResult {

    private AuthResult() {}

    public record TokenPair(
            String accessToken,
            String refreshToken,
            Long userId,
            Long playerId
    ) {}

    public record RegisterResult(
            boolean requiresVerification,
            TokenPair tokenPair
    ) {
        public static RegisterResult verified(TokenPair pair) {
            return new RegisterResult(false, pair);
        }

        public static RegisterResult pendingVerification() {
            return new RegisterResult(true, null);
        }
    }
}
