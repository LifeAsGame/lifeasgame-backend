package online.lifeasgame.character.api.player.mapper;

import online.lifeasgame.character.api.player.response.CertificationResponse;
import online.lifeasgame.character.application.result.CertificationResult;

import java.util.List;

public final class CertificationWebMapper {

    private CertificationWebMapper() {}

    public static CertificationResponse.Infos toInfos(List<CertificationResult.Info> results) {
        return new CertificationResponse.Infos(
                results.stream()
                        .map(
                                result ->
                                        new CertificationResponse.Info(
                                                result.certificationId(),
                                                result.name(),
                                                result.issuer(),
                                                result.category()
                                        )
                        )
                        .toList()
        );
    }

    public static CertificationResponse.Info toInfo(CertificationResult.Info result) {

        return new CertificationResponse.Info(
                result.certificationId(),
                result.name(),
                result.issuer(),
                result.category()
        );
    }
}
