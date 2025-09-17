package online.lifeasgame.character.presentation.mapper;

import online.lifeasgame.character.application.command.AdminPlayerCertificationCommand;
import online.lifeasgame.character.application.result.AdminPlayerCertificationResult;
import online.lifeasgame.character.presentation.request.AdminPlayerCertificationRequest;
import online.lifeasgame.character.presentation.response.AdminPlayerCertificationResponse;

public class AdminPlayerCertificationWebMapper {

    private AdminPlayerCertificationWebMapper() {}

    public static AdminPlayerCertificationResponse.GrantedCertification toGrantedCertification(
            AdminPlayerCertificationResult.GrantedCertification result
    ) {
        return AdminPlayerCertificationResponse.GrantedCertification.of(
                result.playerId(),
                result.certificationId(),
                result.name(),
                result.issuer(),
                result.category(),
                result.acquiredDate(),
                result.expiresDate(),
                result.grantedAt()
        );
    }

    public static AdminPlayerCertificationCommand.GrantCertification toCommand(
            Long playerId,
            Long certificationId,
            AdminPlayerCertificationRequest.GrantCertification request
    ) {
        return AdminPlayerCertificationCommand.GrantCertification.of(
                playerId,
                certificationId,
                request.acquiredDate(),
                request.expiresDate()
        );
    }
}
