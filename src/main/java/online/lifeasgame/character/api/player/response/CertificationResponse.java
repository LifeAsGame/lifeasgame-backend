package online.lifeasgame.character.api.player.response;

import java.util.List;

public class CertificationResponse {

    private CertificationResponse() {
    }

    public record CertificationInfos(
            List<CertificationResponse.CertificationInfo> certificationInfos
    ) {
        public static CertificationResponse.CertificationInfos of(List<CertificationResponse.CertificationInfo> CertificationInfos) {
            return new CertificationResponse.CertificationInfos(CertificationInfos);
        }
    }

    public record CertificationInfo(
            Long certificationId,
            String name,
            String issuer,
            String category
    ) {
        public static CertificationResponse.CertificationInfo of(Long certificationId, String name, String issuer, String category) {
            return new CertificationResponse.CertificationInfo(certificationId, name, issuer, category);
        }
    }
}
