package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Hobby;
import online.lifeasgame.character.domain.HobbyCategory;
import online.lifeasgame.character.domain.error.HobbyError;
import online.lifeasgame.character.domain.repository.HobbyRepository;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class HobbyReader {

    private final HobbyRepository repository;

    public List<Hobby> getHobbies(List<HobbyCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return repository.findAll();
        }
        return repository.findByCategoryIn(categories);
    }

    public Hobby getHobby(Long hobbyId) {
        return repository.findById(hobbyId)
                .orElseThrow(() -> new DomainException(HobbyError.HOBBY_NOT_FOUND));
    }
}
