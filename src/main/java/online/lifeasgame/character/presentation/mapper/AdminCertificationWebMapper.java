package online.lifeasgame.character.presentation.mapper;

import online.lifeasgame.character.application.command.AdminCertificationCommand;
import online.lifeasgame.character.application.result.AdminCertificationResult;
import online.lifeasgame.character.presentation.request.AdminCertificationRequest;
import online.lifeasgame.character.presentation.response.AdminCertificationResponse;

public class AdminCertificationWebMapper {

    public static AdminCertificationCommand.CreateCertification toCommand(AdminCertificationRequest.CreateCertification request) {
        return AdminCertificationCommand.CreateCertification.of(
                request.name(),
                request.issuer(),
                request.category()
        );
    }

    public static AdminCertificationResponse.CertificationInfo toCertificationInfo(
            AdminCertificationResult.CertificationInfo result
    ) {
        return AdminCertificationResponse.CertificationInfo.of(
                result.name(),
                result.issuer(),
                result.category()
        );
    }
}
