package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Title;
import online.lifeasgame.character.domain.repository.TitleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class TitleWriter {

    private final TitleRepository titleRepository;

    public Title create(Title title) {
        return titleRepository.save(title);
    }
}
