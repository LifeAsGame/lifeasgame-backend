package online.lifeasgame.character.infra;

import java.util.Collection;
import java.util.List;
import online.lifeasgame.character.domain.Hobby;
import online.lifeasgame.character.domain.HobbyCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaHobbyRepository extends JpaRepository<Hobby, Long> {
    List<Hobby> findByCategoryIn(Collection<HobbyCategory> categories);
}
