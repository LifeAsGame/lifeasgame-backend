package online.lifeasgame.character.api.player.mapper;

import java.util.List;
import online.lifeasgame.character.application.result.CertificationResult;
import online.lifeasgame.character.api.player.response.CertificationResponse;

public class CertificationWebMapper {

    private CertificationWebMapper() {}

    public static CertificationResponse.CertificationInfos toCertificationInfos(List<CertificationResult.CertificationInfo> certificationInfos) {
        return CertificationResponse.CertificationInfos.of(
                certificationInfos.stream()
                        .map(
                                certificationInfo ->
                                        CertificationResponse.CertificationInfo.of(
                                                certificationInfo.certificationId(),
                                                certificationInfo.name(),
                                                certificationInfo.issuer(),
                                                certificationInfo.category()
                                        )
                        )
                        .toList()
        );
    }
}
