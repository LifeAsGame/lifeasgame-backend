package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.request.AdminCertificationRequest;
import online.lifeasgame.character.api.admin.response.AdminCertificationResponse;
import online.lifeasgame.character.application.command.CertificationCommand;
import online.lifeasgame.character.application.result.CertificationResult;

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
                result.name(),
                result.issuer(),
                result.category()
        );
    }
}
