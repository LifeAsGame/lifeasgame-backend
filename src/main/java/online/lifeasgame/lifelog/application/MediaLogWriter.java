package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.lifelog.application.model.MediaSpec;
import online.lifeasgame.lifelog.domain.MediaLog;
import online.lifeasgame.lifelog.domain.WatchStatus;
import online.lifeasgame.lifelog.domain.repository.MediaLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class MediaLogWriter {

    private final MediaLogRepository repository;

    /**
     * 생성만 save, 변경은 변경감지
     */
    public MediaLog create(MediaSpec.Create spec) {
        MediaLog mediaLog = MediaLog.create(
                spec.playerId(),
                spec.category(),
                spec.title(),
                spec.progress(),
                spec.status(),
                spec.mediaTags()
        );

        return repository.save(mediaLog);
    }

    // 변경감지: 도메인 행위 호출만 수행
    public void rate(MediaLog mediaLog, double score) {
        mediaLog.rate(score);
    }

    public void advance(MediaLog mediaLog, int step) {
        mediaLog.advanceEpisode(step);
    }

    public void markStatus(MediaLog mediaLog, WatchStatus status) {
        mediaLog.markStatus(status);
    }

    public void rewatch(MediaLog mediaLog) {
        mediaLog.rewatch();
    }
}
