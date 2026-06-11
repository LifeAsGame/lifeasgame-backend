package online.lifeasgame.lifelog.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.lifelog.domain.MediaCategory;
import online.lifeasgame.lifelog.domain.MediaLog;
import online.lifeasgame.lifelog.domain.WatchStatus;
import online.lifeasgame.lifelog.domain.repository.MediaLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MediaLogRepositoryAdapter implements MediaLogRepository {

    private final MediaLogJpaRepository jpa;

    @Override
    public MediaLog save(MediaLog mediaLog) {
        return jpa.save(mediaLog);
    }

    @Override
    public Optional<MediaLog> findById(Long id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<MediaLog> findByIdAndPlayerId(Long id, Long playerId) {
        return jpa.findByIdAndPlayerId(id, playerId);
    }

    @Override
    public List<MediaLog> findByPlayerId(Long playerId, int limit) {
        List<MediaLog> base = jpa.findTop100ByPlayerIdOrderByIdDesc(playerId);
        return base.size() <= limit ? base : base.subList(0, limit);
    }

    @Override
    public List<MediaLog> search(
            Long playerId,
            MediaCategory category,
            WatchStatus status,
            String titleLike,
            int page,
            int size
    ) {
        Page<MediaLog> p = jpa.search(
                playerId,
                category,
                status,
                titleLike,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        );
        return p.getContent();
    }

    @Override
    public void deleteByIdAndPlayerId(Long mediaId, Long playerId) {
        jpa.deleteByIdAndPlayerId(mediaId, playerId);
    }
}
