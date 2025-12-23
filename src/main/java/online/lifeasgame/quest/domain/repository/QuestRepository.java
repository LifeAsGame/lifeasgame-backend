package online.lifeasgame.quest.domain.repository;

import online.lifeasgame.quest.domain.Quest;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuestRepository {
    Quest save(Quest quest);

    Optional<Quest> findById(Long questId);

    Optional<Quest> findByCode(String code);

    List<Quest> findAll();

    Collection<Quest> findAllByIds(Collection<Long> questIds);
}
