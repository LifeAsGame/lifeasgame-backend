    package online.lifeasgame.character.api.admin.request;

    import java.time.LocalDate;

    public class AdminPlayerCertificationRequest {

        private AdminPlayerCertificationRequest() {
        }

        public record GrantCertification(
                LocalDate acquiredDate,
                LocalDate expiresDate
        ) {
        }
    }
