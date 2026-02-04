package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.request.AdminCertificationRequest;
import online.lifeasgame.character.api.admin.response.AdminCertificationResponse;
import online.lifeasgame.character.application.command.CertificationCommand;
import online.lifeasgame.character.application.result.CertificationResult;

import java.util.List;

public final class AdminCertificationWebMapper {

    private AdminCertificationWebMapper() {}

    public static CertificationCommand.Create toCreateCommand(AdminCertificationRequest.Create request) {
        return new CertificationCommand.Create(
                request.name(),
                request.issuer(),
                request.category()
        );
    }

    public static AdminCertificationResponse.Info toInfo(CertificationResult.Info result) {
        return new AdminCertificationResponse.Info(
                result.certificationId(),
                result.name(),
                result.issuer(),
                result.category()
        );
    }

    public static AdminCertificationResponse.Infos toInfos(List<CertificationResult.Info> results) {
        return new AdminCertificationResponse.Infos(
                results.stream()
                        .map(
                                result -> new AdminCertificationResponse.Info(
                                        result.certificationId(),
                                        result.name(),
                                        result.issuer(),
                                        result.category()
                                )
                        )
                        .toList()
        );
    }
}
