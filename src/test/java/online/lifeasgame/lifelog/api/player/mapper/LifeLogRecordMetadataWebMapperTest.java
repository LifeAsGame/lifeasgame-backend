package online.lifeasgame.lifelog.api.player.mapper;

import online.lifeasgame.lifelog.api.player.request.PlayerCollectionRequest;
import online.lifeasgame.lifelog.api.player.request.PlayerExerciseRequest;
import online.lifeasgame.lifelog.api.player.request.PlayerMediaLogRequest;
import online.lifeasgame.lifelog.quick.api.QuickRecordRequest;
import online.lifeasgame.lifelog.quick.api.QuickRecordWebMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LifeLog canonical metadata Web mapping")
class LifeLogRecordMetadataWebMapperTest {

    @Test
    @DisplayName("세 direct create API의 nullable metadata를 command로 보존한다")
    void mapsDirectMetadata() {
        var collection = PlayerCollectionWebMapper.toCreateCommand(
                new PlayerCollectionRequest.Create(
                        "BOOK",
                        "title",
                        null,
                        1,
                        null,
                        null,
                        Set.of(),
                        "MEMORY",
                        null
                )
        );
        var exercise = PlayerExerciseWebMapper.toCreateCommand(
                new PlayerExerciseRequest.Create(
                        "RUNNING",
                        10,
                        null,
                        null,
                        LocalDate.of(2026, 7, 29),
                        null,
                        "ACTIVITY",
                        null
                )
        );
        var media = PlayerMediaLogWebMapper.toCreateCommand(
                new PlayerMediaLogRequest.Create(
                        "BOOK",
                        "title",
                        null,
                        0,
                        1,
                        "PLANNED",
                        Set.of(),
                        "STUDY",
                        null
                )
        );

        assertThat(collection.lifeLogMetadata().lifeLogSubtype())
                .isEqualTo("MEMORY");
        assertThat(exercise.lifeLogMetadata().lifeLogSubtype())
                .isEqualTo("ACTIVITY");
        assertThat(media.lifeLogMetadata().lifeLogSubtype())
                .isEqualTo("STUDY");
    }

    @Test
    @DisplayName("기존 direct client 입력은 metadata null command로 계속 mapping한다")
    void mapsLegacyDirectInputWithoutMetadata() {
        var command = PlayerCollectionWebMapper.toCreateCommand(
                new PlayerCollectionRequest.Create(
                        "BOOK",
                        "title",
                        null,
                        1,
                        null,
                        null,
                        Set.of()
                )
        );

        assertThat(command.lifeLogMetadata().isPresent()).isFalse();
    }

    @Test
    @DisplayName("Quick Record는 top-level metadata만 selected command로 전달한다")
    void mapsQuickTopLevelMetadata() {
        var command = QuickRecordWebMapper.toCommand(
                new QuickRecordRequest.Create(
                        "COLLECTION",
                        "REFLECTION",
                        "WEEKLY_LOOKBACK",
                        new PlayerCollectionRequest.Create(
                                "BOOK",
                                "weekly",
                                null,
                                1,
                                null,
                                null,
                                Set.of()
                        ),
                        null,
                        null
                )
        ).selected();

        assertThat(command.lifeLogMetadata().lifeLogSubtype())
                .isEqualTo("REFLECTION");
        assertThat(command.lifeLogMetadata().reflectionScope())
                .isEqualTo("WEEKLY_LOOKBACK");
    }

    @Nested
    @DisplayName("Role/Event flat field를 create command로 옮길 때")
    class MapRoleContext {

        @Test
        @DisplayName("Collection/Exercise/Media 모두 동일한 canonical metadata를 보존한다")
        void mapsAllDirectCreateRoleContexts() {
            var collection = PlayerCollectionWebMapper.toCreateCommand(
                    new PlayerCollectionRequest.Create(
                            "BOOK", "title", null, 1, null, null,
                            Set.of(), null, null, 10L, 20L
                    )
            );
            var exercise = PlayerExerciseWebMapper.toCreateCommand(
                    new PlayerExerciseRequest.Create(
                            "RUNNING", 10, null, null,
                            LocalDate.of(2026, 8, 11), null,
                            null, null, 10L, 20L
                    )
            );
            var media = PlayerMediaLogWebMapper.toCreateCommand(
                    new PlayerMediaLogRequest.Create(
                            "BOOK", "title", null, 0, 1, "PLANNED",
                            Set.of(), null, null, 10L, 20L
                    )
            );

            assertThat(List.of(
                    collection.lifeLogMetadata(),
                    exercise.lifeLogMetadata(),
                    media.lifeLogMetadata()
            )).allSatisfy(metadata -> {
                assertThat(metadata.primaryRoleId()).isEqualTo(10L);
                assertThat(metadata.roleEventId()).isEqualTo(20L);
                assertThat(metadata.lifeLogSubtype()).isNull();
            });
        }

        @Test
        @DisplayName("Quick Record는 top-level Role/Event만 selected metadata로 전달한다")
        void mapsQuickTopLevelRoleContext() {
            var selected = QuickRecordWebMapper.toCommand(
                    new QuickRecordRequest.Create(
                            "COLLECTION",
                            null,
                            null,
                            10L,
                            20L,
                            new PlayerCollectionRequest.Create(
                                    "BOOK", "title", null, 1,
                                    null, null, Set.of()
                            ),
                            null,
                            null
                    )
            ).selected();

            assertThat(selected.lifeLogMetadata().primaryRoleId())
                    .isEqualTo(10L);
            assertThat(selected.lifeLogMetadata().roleEventId())
                    .isEqualTo(20L);
        }
    }
}
