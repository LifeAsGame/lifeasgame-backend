package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.request.AdminPlayerCertificationRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerCertificationResponse;
import online.lifeasgame.character.application.command.PlayerCertificationCommand;
import online.lifeasgame.character.application.result.PlayerCertificationResult;

public final class AdminPlayerCertificationWebMapper {

    private AdminPlayerCertificationWebMapper() {}

    public static PlayerCertificationCommand.Grant toGrantCommand(
            Long playerId,
            Long certificationId,
            AdminPlayerCertificationRequest.Grant request
    ) {
        return new PlayerCertificationCommand.Grant(
                playerId,
                certificationId,
                request.acquiredDate(),
                request.expiresDate()
        );
    }

    public static AdminPlayerCertificationResponse.Granted toGranted(
            PlayerCertificationResult.Granted result
    ) {
        return new AdminPlayerCertificationResponse.Granted(
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
}
