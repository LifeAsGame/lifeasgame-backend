package online.lifeasgame.character.presentation.mapper;

import java.util.List;
import online.lifeasgame.character.application.result.PlayerCertificationResult;
import online.lifeasgame.character.presentation.response.PlayerCertificationResponse;

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
}

