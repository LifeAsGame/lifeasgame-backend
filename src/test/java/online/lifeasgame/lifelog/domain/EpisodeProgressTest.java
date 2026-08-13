package online.lifeasgame.lifelog.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EpisodeProgress optional binding")
class EpisodeProgressTest {

    @Nested
    @DisplayName("create 입력을 정규화할 때")
    class NormalizeCreateInput {

        @Test
        @DisplayName("current와 total이 모두 없으면 0/1이다")
        void defaultsBoth() {
            assertProgress(EpisodeProgress.of(null, null), 0, 1);
        }

        @Test
        @DisplayName("total만 있으면 0/T이다")
        void defaultsCurrent() {
            assertProgress(EpisodeProgress.of(null, 12), 0, 12);
        }

        @Test
        @DisplayName("current 0만 있으면 0/1이다")
        void defaultsTotalForZeroCurrent() {
            assertProgress(EpisodeProgress.of(0, null), 0, 1);
        }

        @Test
        @DisplayName("양수 current만 있으면 C/C이다")
        void defaultsTotalToCurrent() {
            assertProgress(EpisodeProgress.of(7, null), 7, 7);
        }

        @Test
        @DisplayName("둘 다 있으면 값을 그대로 보존한다")
        void preservesExplicitProgress() {
            assertProgress(EpisodeProgress.of(3, 10), 3, 10);
        }

        @Test
        @DisplayName("current가 supplied total보다 크면 기존 invariant 실패다")
        void rejectsCurrentAboveTotal() {
            assertThatThrownBy(() -> EpisodeProgress.of(4, 3))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("current > total");
        }
    }

    @Nested
    @DisplayName("기존 Media 동작을 사용할 때")
    class ExistingMediaBehavior {

        @Test
        @DisplayName("normal create/update/advance의 명시적 progress 의미를 보존한다")
        void preservesCreateUpdateAndAdvance() {
            MediaLog media = MediaLog.create(
                    266L,
                    MediaCategory.MOVIE,
                    Title.of("title", null),
                    EpisodeProgress.of(1, 4),
                    WatchStatus.PLANNED,
                    MediaTags.of(Set.of())
            );
            media.update(
                    MediaCategory.MOVIE,
                    Title.of("updated", null),
                    EpisodeProgress.of(2, 5),
                    WatchStatus.WATCHING,
                    MediaTags.of(Set.of())
            );

            media.advanceEpisode(2);

            assertProgress(media.getProgress(), 4, 5);
            assertThat(media.getStatus()).isEqualTo(WatchStatus.WATCHING);
            assertThat(media.getTitle().value()).isEqualTo("updated");
        }
    }

    private static void assertProgress(
            EpisodeProgress progress,
            int current,
            int total
    ) {
        assertThat(progress.current()).isEqualTo(current);
        assertThat(progress.total()).isEqualTo(total);
    }
}
