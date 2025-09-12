package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.PlayerTitle;
import online.lifeasgame.character.domain.error.PlayerTitleError;
import online.lifeasgame.character.domain.repository.PlayerTitleRepository;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class PlayerTitleWriter {

    private final PlayerTitleRepository playerTitleRepository;

    public PlayerTitle grantTitle(PlayerTitle playerTitle) {
        if (playerTitleRepository.existsByPlayerIdAndTitleId(playerTitle.getPlayerId(), playerTitle.getTitleId())) {
            throw new DomainException(PlayerTitleError.PLAYER_TITLE_ALREADY_EXISTS);
        }

        return playerTitleRepository.save(playerTitle);
    }
}
