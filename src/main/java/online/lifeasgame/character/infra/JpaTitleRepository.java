package online.lifeasgame.character.infra;

import java.util.Collection;
import java.util.List;
import online.lifeasgame.character.domain.Title;
import online.lifeasgame.character.domain.TitleCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaTitleRepository extends JpaRepository<Title, Long> {
    List<Title> findByCategoryIn(Collection<TitleCategory> categories);
}
