package online.lifeasgame.character.domain.repository;

import java.util.List;
import online.lifeasgame.character.domain.Title;
import online.lifeasgame.character.domain.TitleCategory;

public interface TitleRepository {
    List<Title> findByCategoryIn(List<TitleCategory> categories);

    List<Title> findAll();
}
