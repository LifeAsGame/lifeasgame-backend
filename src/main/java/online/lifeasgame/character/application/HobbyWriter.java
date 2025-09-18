package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Hobby;
import online.lifeasgame.character.domain.repository.HobbyRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class HobbyWriter {

    private final HobbyRepository repository;

    public Hobby create(Hobby hobby) {
        return repository.save(hobby);
    }
}
