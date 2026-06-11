package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Hobby;
import online.lifeasgame.character.domain.HobbyCategory;
import online.lifeasgame.character.domain.error.HobbyError;
import online.lifeasgame.character.domain.repository.HobbyRepository;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class HobbyReader {

    private final HobbyRepository repository;

    public List<Hobby> getByCategories(List<HobbyCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return repository.findAll();
        }

        return repository.findByCategoryIn(categories);
    }

    public Hobby getByIdOrThrow(Long hobbyId) {
        return repository.findById(hobbyId)
                .orElseThrow(() -> new DomainException(HobbyError.HOBBY_NOT_FOUND));
    }
}
