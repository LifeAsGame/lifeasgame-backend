package online.lifeasgame.character.domain.repository;

import java.util.List;
import java.util.Optional;
import online.lifeasgame.character.domain.Title;
import online.lifeasgame.character.domain.TitleCategory;

public interface TitleRepository {
    List<Title> findByCategoryIn(List<TitleCategory> categories);

    List<Title> findAll();

    Title save(Title title);

    Optional<Title> findById(Long titleId);
}
