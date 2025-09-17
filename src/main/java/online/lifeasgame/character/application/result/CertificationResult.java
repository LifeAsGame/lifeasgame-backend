package online.lifeasgame.character.application.result;

import java.util.List;
import online.lifeasgame.character.domain.Certification;

public class CertificationResult {

    private CertificationResult() {
    }

    public record CertificationInfo(
            String name,
            String issuer,
            String category
    ) {
        public static CertificationInfo from(Certification certification) {
            return new CertificationInfo(
                    certification.getName(),
                    certification.getIssuer(),
                    certification.getCategory().name()
            );
        }

        public static List<CertificationInfo> fromList(List<Certification> Certifications) {
            return Certifications.stream().map(CertificationResult.CertificationInfo::from).toList();
        }
    }
}
