package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.request.AdminPlayerCertificationRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerCertificationResponse;
import online.lifeasgame.character.application.command.PlayerCertificationCommand;
import online.lifeasgame.character.application.result.PlayerCertificationResult;

public class AdminPlayerCertificationWebMapper {

    private AdminPlayerCertificationWebMapper() {}

    public static AdminPlayerCertificationResponse.Granted toGrantedCertification(
            PlayerCertificationResult.Granted result
    ) {
        return AdminPlayerCertificationResponse.Granted.of(
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

    public static PlayerCertificationCommand.Grant toCommand(
            Long playerId,
            Long certificationId,
            AdminPlayerCertificationRequest.Grant request
    ) {
        return PlayerCertificationCommand.Grant.of(
                playerId,
                certificationId,
                request.acquiredDate(),
                request.expiresDate()
        );
    }
}
