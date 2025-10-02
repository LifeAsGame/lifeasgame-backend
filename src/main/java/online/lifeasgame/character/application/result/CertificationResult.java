package online.lifeasgame.character.application.result;

import online.lifeasgame.character.domain.Certification;

import java.util.List;

public final class CertificationResult {

    private CertificationResult() {
    }

    public record Info(
            Long certificationId,
            String name,
            String issuer,
            String category
    ) {
        public static Info from(Certification certification) {
            return new Info(
                    certification.getId(),
                    certification.getName(),
                    certification.getIssuer(),
                    certification.getCategory().name()
            );
        }

        public static List<Info> fromList(List<Certification> Certifications) {
            return Certifications.stream().map(Info::from).toList();
        }

        public static Info of(Long certificationId, String name, String issuer, String category) {
            return new Info(certificationId, name, issuer, category);
        }
    }
}
