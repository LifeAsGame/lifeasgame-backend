package online.lifeasgame.lifelog.domain.repository;

import online.lifeasgame.lifelog.domain.MediaLog;

import java.util.Optional;

public interface MediaLogRepository {

    MediaLog save(MediaLog mediaLog);

    Optional<MediaLog> findById(Long id);

    Optional<MediaLog> findByIdAndPlayerId(Long id, Long playerId);
}
