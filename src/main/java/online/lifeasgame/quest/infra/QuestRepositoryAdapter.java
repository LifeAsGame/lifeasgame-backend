package online.lifeasgame.quest.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.quest.domain.Quest;
import online.lifeasgame.quest.domain.repository.QuestRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class QuestRepositoryAdapter implements QuestRepository {

    private final JpaQuestRepository jpaQuestRepository;

    @Override
    public Quest save(Quest quest) {
        return jpaQuestRepository.save(quest);
    }

    @Override
    public Optional<Quest> findById(Long questId) {
        return jpaQuestRepository.findById(questId);
    }

    @Override
    public Optional<Quest> findByCode(String code) {
        return jpaQuestRepository.findByCode(code);
    }

    @Override
    public List<Quest> findAll() {
        return jpaQuestRepository.findAll();
    }

    @Override
    public Collection<Quest> findAllByIds(Collection<Long> questIds) {
        return jpaQuestRepository.findAllById(questIds);
    }
}
