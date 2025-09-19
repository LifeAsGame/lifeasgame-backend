package online.lifeasgame.character.domain.repository;

import java.util.List;
import java.util.Optional;
import online.lifeasgame.character.domain.Hobby;
import online.lifeasgame.character.domain.HobbyCategory;

public interface HobbyRepository {
    Hobby save(Hobby hobby);

    List<Hobby> findAll();

    List<Hobby> findByCategoryIn(List<HobbyCategory> categories);

    Optional<Hobby> findById(Long id);
}
