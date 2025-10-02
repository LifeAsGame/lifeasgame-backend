package online.lifeasgame.character.api.player.mapper;

import java.util.List;
import online.lifeasgame.character.application.command.PlayerCertificationCommand;
import online.lifeasgame.character.application.result.PlayerCertificationResult;
import online.lifeasgame.character.application.result.PlayerCertificationResult.CreatedPlayerCertification;
import online.lifeasgame.character.api.player.request.PlayerCertificationRequest;
import online.lifeasgame.character.api.player.response.PlayerCertificationResponse;

public class PlayerCertificationWebMapper {

    private PlayerCertificationWebMapper() {}

    public static PlayerCertificationResponse.PlayerCertificationInfos toPlayerCertificationInfos(List<PlayerCertificationResult.PlayerCertificationInfo> playerCertificationInfos) {
        return PlayerCertificationResponse.PlayerCertificationInfos.of(
                playerCertificationInfos.stream()
                        .map(
                                playerCertificationInfo ->
                                        PlayerCertificationResponse.PlayerCertificationInfo.of(
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

    public static PlayerCertificationCommand.ChangePlayerCertification toCommand(
            Long certificationId,
            PlayerCertificationRequest.ChangePlayerCertification request
    ) {
        return PlayerCertificationCommand.ChangePlayerCertification.of(
                certificationId,
                request.acquiredDate(),
                request.expiresDate()
        );
    }

    public static PlayerCertificationCommand.CreatePlayerCertification toCommand(
            Long certificationId,
            PlayerCertificationRequest.CreatePlayerCertification request
    ) {
        return PlayerCertificationCommand.CreatePlayerCertification.of(
                certificationId,
                request.acquiredDate(),
                request.expiresDate()
        );
    }

    public static PlayerCertificationResponse.ChangedPlayerCertification toChangedPlayerCertification(
            PlayerCertificationResult.ChangedPlayerCertification result
    ) {
        return PlayerCertificationResponse.ChangedPlayerCertification.of(
                result.certificationId(),
                result.acquiredDate(),
                result.expiresDate()
        );
    }

    public static PlayerCertificationResponse.CreatedPlayerCertification toCreatedPlayerCertification(
            CreatedPlayerCertification certificationInfo
    ) {
        return PlayerCertificationResponse.CreatedPlayerCertification.of(
                certificationInfo.certificationId(),
                certificationInfo.acquiredDate(),
                certificationInfo.expiresDate()
        );
    }
}
