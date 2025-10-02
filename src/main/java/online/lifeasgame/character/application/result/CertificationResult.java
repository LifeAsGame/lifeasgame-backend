package online.lifeasgame.character.application.result;

import online.lifeasgame.character.domain.Certification;

import java.util.List;

public class CertificationResult {

    private CertificationResult() {
    }

    public record CertificationInfo(
            Long certificationId,
            String name,
            String issuer,
            String category
    ) {
        public static CertificationInfo from(Certification certification) {
            return new CertificationInfo(
                    certification.getId(),
                    certification.getName(),
                    certification.getIssuer(),
                    certification.getCategory().name()
            );
        }

        public static List<CertificationInfo> fromList(List<Certification> Certifications) {
            return Certifications.stream().map(CertificationResult.CertificationInfo::from).toList();
        }

        public static CertificationInfo of(Long certificationId, String name, String issuer, String category) {
            return new CertificationInfo(certificationId, name, issuer, category);
        }
    }
}
