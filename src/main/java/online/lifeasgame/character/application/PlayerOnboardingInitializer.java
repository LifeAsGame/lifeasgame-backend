package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.command.PlayerCommand;
import online.lifeasgame.character.application.result.PlayerResult;
import online.lifeasgame.character.domain.GenderType;
import online.lifeasgame.character.domain.Name;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerOnboardingInitializer {

    private final PlayerWriter playerWriter;
    private final PlayerReader playerReader;
    private final PlayerEquipmentProvisioner equipmentProvisioner;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public PlayerResult.Created initialize(
            Long userId,
            PlayerCommand.Register register
    ) {
        Player existing = playerReader.getByUserId(userId);
        if (existing != null) {
            Player locked = playerReader.getByUserIdForUpdateOrThrow(userId);
            equipmentProvisioner.provisionAndVerify(locked.getId());
            return new PlayerResult.Created(locked.getId());
        }

        Player player = Player.linkStart(
                userId,
                Name.of(register.name()),
                GenderType.parse(register.gender())
        );
        try {
            playerWriter.create(player);
        } catch (DataIntegrityViolationException conflict) {
            throw new DomainException(
                    PlayerError.PLAYER_ALREADY_EXISTS,
                    null,
                    conflict
            );
        }
        equipmentProvisioner.provisionAndVerify(player.getId());
        domainEventPublisher.publishAll(player.pullEvents());
        return new PlayerResult.Created(player.getId());
    }
}
