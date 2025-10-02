package online.lifeasgame.character.api.player.mapper;

import java.util.List;
import online.lifeasgame.character.application.result.CertificationResult;
import online.lifeasgame.character.api.player.response.CertificationResponse;

public class CertificationWebMapper {

    private CertificationWebMapper() {}

    public static CertificationResponse.Infos toCertificationInfos(List<CertificationResult.Info> infos) {
        return CertificationResponse.Infos.of(
                infos.stream()
                        .map(
                                certificationInfo ->
                                        CertificationResponse.Info.of(
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
