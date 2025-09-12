package online.lifeasgame.character.infra;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Title;
import online.lifeasgame.character.domain.TitleCategory;
import online.lifeasgame.character.domain.repository.TitleRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TitleRepositoryAdapter implements TitleRepository {

    private final JpaTitleRepository jpaRepository;

    @Override
    public List<Title> findByCategoryIn(List<TitleCategory> titleCategories) {
        return jpaRepository.findByCategoryIn(titleCategories);
    }

    @Override
    public List<Title> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public Title save(Title title) {
        return jpaRepository.save(title);
    }

    @Override
    public Optional<Title> findById(Long id) {
        return jpaRepository.findById(id);
    }
}
