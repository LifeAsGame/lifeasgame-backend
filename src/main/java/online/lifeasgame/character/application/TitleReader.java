package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Title;
import online.lifeasgame.character.domain.TitleCategory;
import online.lifeasgame.character.domain.error.TitleError;
import online.lifeasgame.character.domain.repository.TitleRepository;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class TitleReader {

    private final TitleRepository repository;

    public List<Title> getTitles(List<TitleCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return repository.findAll();
        }
        return repository.findByCategoryIn(categories);
    }

    public Title getTitle(Long titleId) {
        return repository.findById(titleId)
                .orElseThrow(() -> new DomainException(TitleError.TITLE_NOT_FOUND));
    }
}
