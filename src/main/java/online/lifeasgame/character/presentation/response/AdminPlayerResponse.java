package online.lifeasgame.character.presentation.response;

public class AdminPlayerResponse {

    private AdminPlayerResponse() {
    }

    public record ExpGranted(
            Long playerId,
            long requestedExp,
            long appliedExp,
            long leftoverExp,
            int level,
            long totalExp,
            long currentExp,
            long restExp,
            long capacityToNextLevel,
            double progressRatio
    ) {
        public static AdminPlayerResponse.ExpGranted of(
                Long playerId,
                long requestedExp,
                long appliedExp,
                long leftoverExp,
                int level,
                long totalExp,
                long currentExp,
                long restExp,
                long capacityToNextLevel,
                double progressRatio
        ) {
            return new AdminPlayerResponse.ExpGranted(
                    playerId,
                    requestedExp,
                    appliedExp,
                    leftoverExp,
                    level,
                    totalExp,
                    currentExp,
                    restExp,
                    capacityToNextLevel,
                    progressRatio
            );
        }
    }
}
