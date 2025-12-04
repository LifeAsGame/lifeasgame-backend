package online.lifeasgame.character.api.player.mapper;

import online.lifeasgame.character.api.player.request.PlayerCertificationRequest;
import online.lifeasgame.character.api.player.response.PlayerCertificationResponse;
import online.lifeasgame.character.application.command.PlayerCertificationCommand;
import online.lifeasgame.character.application.result.PlayerCertificationResult;

import java.util.List;

public class PlayerCertificationWebMapper {

    private PlayerCertificationWebMapper() {}

    public static PlayerCertificationResponse.Infos toInfos(List<PlayerCertificationResult.Info> results) {
        return new PlayerCertificationResponse.Infos(
                results.stream()
                        .map(
                                result ->
                                        new PlayerCertificationResponse.Info(
                                                result.certificationId(),
                                                result.name(),
                                                result.issuer(),
                                                result.category(),
                                                result.acquiredDate(),
                                                result.expiresDate(),
                                                result.grantedAt()
                                        )
                        )
                        .toList()
        );
    }

    public static PlayerCertificationCommand.Change toChangeCommand(
            Long certificationId,
            PlayerCertificationRequest.Change request
    ) {
        return new PlayerCertificationCommand.Change(
                certificationId,
                request.acquiredDate(),
                request.expiresDate()
        );
    }

    public static PlayerCertificationResponse.Changed toChanged(
            PlayerCertificationResult.Changed result
    ) {
        return new PlayerCertificationResponse.Changed(
                result.certificationId(),
                result.acquiredDate(),
                result.expiresDate()
        );
    }

    public static PlayerCertificationCommand.Create toCreateCommand(
            Long certificationId,
            PlayerCertificationRequest.Create request
    ) {
        return new PlayerCertificationCommand.Create(
                certificationId,
                request.acquiredDate(),
                request.expiresDate()
        );
    }

    public static PlayerCertificationResponse.Created toCreated(
            PlayerCertificationResult.Created result
    ) {
        return new PlayerCertificationResponse.Created(
                result.certificationId(),
                result.acquiredDate(),
                result.expiresDate()
        );
    }
}
