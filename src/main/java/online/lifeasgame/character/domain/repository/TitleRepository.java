package online.lifeasgame.character.domain.repository;

import online.lifeasgame.character.domain.Title;
import online.lifeasgame.character.domain.TitleCategory;

import java.util.List;
import java.util.Optional;

public interface TitleRepository {
    List<Title> findByCategoryIn(List<TitleCategory> categories);

    List<Title> findAll();

    Title save(Title title);

    Optional<Title> findById(Long titleId);

    void deleteById(Long titleId);
}
