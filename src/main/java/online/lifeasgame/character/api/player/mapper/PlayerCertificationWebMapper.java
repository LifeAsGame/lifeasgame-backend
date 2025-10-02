package online.lifeasgame.character.api.player.mapper;

import java.util.List;
import online.lifeasgame.character.application.command.PlayerCertificationCommand;
import online.lifeasgame.character.application.result.PlayerCertificationResult;
import online.lifeasgame.character.api.player.request.PlayerCertificationRequest;
import online.lifeasgame.character.api.player.response.PlayerCertificationResponse;

public class PlayerCertificationWebMapper {

    private PlayerCertificationWebMapper() {}

    public static PlayerCertificationResponse.Infos toPlayerCertificationInfos(List<PlayerCertificationResult.Info> infos) {
        return PlayerCertificationResponse.Infos.of(
                infos.stream()
                        .map(
                                playerCertificationInfo ->
                                        PlayerCertificationResponse.Info.of(
                                                playerCertificationInfo.certificationId(),
                                                playerCertificationInfo.name(),
                                                playerCertificationInfo.issuer(),
                                                playerCertificationInfo.category(),
                                                playerCertificationInfo.acquiredDate(),
                                                playerCertificationInfo.expiresDate(),
                                                playerCertificationInfo.grantedAt()
                                        )
                        )
                        .toList()
        );
    }

    public static PlayerCertificationCommand.Change toCommand(
            Long certificationId,
            PlayerCertificationRequest.Change request
    ) {
        return PlayerCertificationCommand.Change.of(
                certificationId,
                request.acquiredDate(),
                request.expiresDate()
        );
    }

    public static PlayerCertificationCommand.Create toCommand(
            Long certificationId,
            PlayerCertificationRequest.Create request
    ) {
        return PlayerCertificationCommand.Create.of(
                certificationId,
                request.acquiredDate(),
                request.expiresDate()
        );
    }

    public static PlayerCertificationResponse.Changed toChangedPlayerCertification(
            PlayerCertificationResult.Changed result
    ) {
        return PlayerCertificationResponse.Changed.of(
                result.certificationId(),
                result.acquiredDate(),
                result.expiresDate()
        );
    }

    public static PlayerCertificationResponse.Created toCreatedPlayerCertification(
            PlayerCertificationResult.Created certificationInfo
    ) {
        return PlayerCertificationResponse.Created.of(
                certificationInfo.certificationId(),
                certificationInfo.acquiredDate(),
                certificationInfo.expiresDate()
        );
    }
}
