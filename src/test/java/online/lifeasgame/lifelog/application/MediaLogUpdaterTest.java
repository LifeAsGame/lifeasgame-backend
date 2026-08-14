package online.lifeasgame.lifelog.application;

import online.lifeasgame.lifelog.application.command.MediaLogCommand;
import online.lifeasgame.lifelog.domain.EpisodeProgress;
import online.lifeasgame.lifelog.domain.MediaCategory;
import online.lifeasgame.lifelog.domain.MediaLog;
import online.lifeasgame.lifelog.domain.MediaTags;
import online.lifeasgame.lifelog.domain.Title;
import online.lifeasgame.lifelog.domain.WatchStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaLogUpdater")
class MediaLogUpdaterTest {

    private static final Long PLAYER_ID = 276L;
    private static final Long MEDIA_ID = 2761L;

    @Mock
    private MediaLogReader mediaLogReader;

    private MediaLogUpdater updater;
    private MediaLog mediaLog;

    @BeforeEach
    void setUp() {
        updater = new MediaLogUpdater(mediaLogReader);
        mediaLog = MediaLog.create(
                PLAYER_ID,
                MediaCategory.SERIES,
                Title.of("기존 제목", "기존 원제"),
                EpisodeProgress.of(3, 12),
                WatchStatus.WATCHING,
                MediaTags.of(Set.of("drama", "favorite"))
        );
        given(mediaLogReader.getByPlayerIdAndIdOrThrow(PLAYER_ID, MEDIA_ID))
                .willReturn(mediaLog);
    }

    @Nested
    @DisplayName("미디어 기록을 부분 수정할 때")
    class UpdateMediaLog {

        @Test
        @DisplayName("전달한 category만 바꾸고 생략한 title/progress/status/tags는 보존한다")
        void appliesSubsetAndPreservesOmittedFields() {
            updater.update(
                    PLAYER_ID,
                    MEDIA_ID,
                    new MediaLogCommand.Update("MOVIE", null, null, null, null, null, null)
            );

            assertThat(mediaLog.getCategory()).isEqualTo(MediaCategory.MOVIE);
            assertThat(mediaLog.getTitle()).isEqualTo(Title.of("기존 제목", "기존 원제"));
            assertThat(mediaLog.getProgress()).isEqualTo(EpisodeProgress.of(3, 12));
            assertThat(mediaLog.getStatus()).isEqualTo(WatchStatus.WATCHING);
            assertThat(mediaLog.getMediaTags().values()).containsExactlyInAnyOrder("drama", "favorite");
        }

        @Test
        @DisplayName("currentEpisode만 전달하면 totalEpisode는 보존한다")
        void mergesProgressAgainstPersistedValues() {
            updater.update(
                    PLAYER_ID,
                    MEDIA_ID,
                    new MediaLogCommand.Update(null, null, null, 5, null, null, null)
            );

            assertThat(mediaLog.getProgress()).isEqualTo(EpisodeProgress.of(5, 12));
        }

        @Test
        @DisplayName("null tags는 보존하고 빈 tags는 지운다")
        void preservesNullTagsAndClearsEmptyTags() {
            updater.update(
                    PLAYER_ID,
                    MEDIA_ID,
                    new MediaLogCommand.Update(null, null, null, null, null, null, null)
            );

            assertThat(mediaLog.getMediaTags().values()).containsExactlyInAnyOrder("drama", "favorite");

            updater.update(
                    PLAYER_ID,
                    MEDIA_ID,
                    new MediaLogCommand.Update(null, null, null, null, null, null, Set.of())
            );

            assertThat(mediaLog.getMediaTags().values()).isEmpty();
        }

        @Test
        @DisplayName("null originalTitle은 보존하고 blank originalTitle은 지운다")
        void preservesNullOriginalTitleAndClearsBlankOriginalTitle() {
            updater.update(
                    PLAYER_ID,
                    MEDIA_ID,
                    new MediaLogCommand.Update(null, null, null, null, null, null, null)
            );

            assertThat(mediaLog.getTitle().original()).isEqualTo("기존 원제");

            updater.update(
                    PLAYER_ID,
                    MEDIA_ID,
                    new MediaLogCommand.Update(null, null, "   ", null, null, null, null)
            );

            assertThat(mediaLog.getTitle().original()).isNull();
        }

        @Test
        @DisplayName("병합한 progress가 유효하지 않으면 다른 필드도 변경하지 않는다")
        void rejectsInvalidProgressWithoutPartialMutation() {
            assertThatThrownBy(() -> updater.update(
                    PLAYER_ID,
                    MEDIA_ID,
                    new MediaLogCommand.Update("MOVIE", "변경 제목", null, 13, null, "COMPLETED", Set.of())
            )).isInstanceOf(IllegalStateException.class)
                    .hasMessage("current > total");

            assertThat(mediaLog.getCategory()).isEqualTo(MediaCategory.SERIES);
            assertThat(mediaLog.getTitle()).isEqualTo(Title.of("기존 제목", "기존 원제"));
            assertThat(mediaLog.getProgress()).isEqualTo(EpisodeProgress.of(3, 12));
            assertThat(mediaLog.getStatus()).isEqualTo(WatchStatus.WATCHING);
            assertThat(mediaLog.getMediaTags().values()).containsExactlyInAnyOrder("drama", "favorite");
        }
    }
}
