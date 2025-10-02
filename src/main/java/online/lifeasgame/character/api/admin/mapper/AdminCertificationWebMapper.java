package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.request.AdminCertificationRequest;
import online.lifeasgame.character.api.admin.response.AdminCertificationResponse;
import online.lifeasgame.character.application.command.CertificationCommand;
import online.lifeasgame.character.application.result.CertificationResult;

public class AdminCertificationWebMapper {

    public static CertificationCommand.Create toCommand(AdminCertificationRequest.CreateCertification request) {
        return CertificationCommand.Create.of(
                request.name(),
                request.issuer(),
                request.category()
        );
    }

    public static AdminCertificationResponse.CertificationInfo toCertificationInfo(
            CertificationResult.CertificationInfo result
    ) {
        return AdminCertificationResponse.CertificationInfo.of(
                result.name(),
                result.issuer(),
                result.category()
        );
    }
}
