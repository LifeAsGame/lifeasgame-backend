package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.request.AdminPlayerCertificationRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerCertificationResponse;
import online.lifeasgame.character.application.command.PlayerCertificationCommand;
import online.lifeasgame.character.application.result.PlayerCertificationResult;

import java.util.List;

public final class AdminPlayerCertificationWebMapper {

    private AdminPlayerCertificationWebMapper() {}

    public static AdminPlayerCertificationResponse.Infos toInfos(
            Long playerId,
            List<PlayerCertificationResult.Info> results
    ) {
        return new AdminPlayerCertificationResponse.Infos(
                playerId,
                results.stream()
                        .map(result -> new AdminPlayerCertificationResponse.Info(
                                result.certificationId(),
                                result.name(),
                                result.issuer(),
                                result.category(),
                                result.acquiredDate(),
                                result.expiresDate(),
                                result.grantedAt()
                        ))
                        .toList()
        );
    }

    public static PlayerCertificationCommand.Create toCreateCommand(
            Long certificationId,
            AdminPlayerCertificationRequest.Create request
    ) {
        return new PlayerCertificationCommand.Create(
                certificationId,
                request.acquiredDate(),
                request.expiresDate()
        );
    }

    public static AdminPlayerCertificationResponse.Granted toGranted(
            PlayerCertificationResult.Created result
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

    public static AdminPlayerCertificationResponse.Revoked toRevoked(PlayerCertificationResult.Revoked result) {
        return new AdminPlayerCertificationResponse.Revoked(result.playerId(), result.certificationId());
    }
}
