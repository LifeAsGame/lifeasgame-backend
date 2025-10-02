package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.request.AdminPlayerCertificationRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerCertificationResponse;
import online.lifeasgame.character.application.command.PlayerCertificationCommand;
import online.lifeasgame.character.application.result.PlayerCertificationResult;

public class AdminPlayerCertificationWebMapper {

    private AdminPlayerCertificationWebMapper() {}

    public static AdminPlayerCertificationResponse.GrantedCertification toGrantedCertification(
            PlayerCertificationResult.GrantedCertification result
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

    public static PlayerCertificationCommand.GrantCertification toCommand(
            Long playerId,
            Long certificationId,
            AdminPlayerCertificationRequest.GrantCertification request
    ) {
        return PlayerCertificationCommand.GrantCertification.of(
                playerId,
                certificationId,
                request.acquiredDate(),
                request.expiresDate()
        );
    }
}
