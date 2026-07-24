package online.lifeasgame.lifelog.quick.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.lifelog.application.command.CollectionCommand;
import online.lifeasgame.lifelog.application.command.ExerciseCommand;
import online.lifeasgame.lifelog.application.command.MediaLogCommand;
import online.lifeasgame.lifelog.domain.event.LifeLogType;
import online.lifeasgame.lifelog.quick.domain.error.QuickRecordError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("QuickRecord request selection과 canonical hash")
class QuickRecordRequestHasherTest {

    private final QuickRecordRequestHasher hasher =
            new QuickRecordRequestHasher();

    @Test
    @DisplayName("type과 일치하는 payload 하나만 선택한다")
    void selectsExactlyOneMatchingPayload() {
        QuickRecordCommand.Selected selected = new QuickRecordCommand.Create(
                " collection ",
                collection("title", Set.of("tag")),
                null,
                null
        ).selected();

        assertThat(selected.type()).isEqualTo(LifeLogType.COLLECTION);
        assertThat(selected.collection()).isNotNull();
        assertThat(selected.exercise()).isNull();
        assertThat(selected.media()).isNull();
    }

    @Test
    @DisplayName("payload 없음, 복수 payload, type 불일치를 모두 거부한다")
    void rejectsInvalidPayloadShape() {
        assertInvalid(new QuickRecordCommand.Create(
                "COLLECTION", null, null, null
        ));
        assertInvalid(new QuickRecordCommand.Create(
                "COLLECTION",
                collection("title", Set.of()),
                exercise(),
                null
        ));
        assertInvalid(new QuickRecordCommand.Create(
                "COLLECTION", null, exercise(), null
        ));
        assertInvalid(new QuickRecordCommand.Create(
                "DIARY",
                collection("title", Set.of()),
                null,
                null
        ));
    }

    @Test
    @DisplayName("공백, enum 표기, tags 순서와 중복 정규화 후 hash가 같다")
    void normalizesCollectionPayload() {
        Set<String> firstTags = new LinkedHashSet<>();
        firstTags.add(" Daily ");
        firstTags.add("BOOK");
        Set<String> secondTags = new LinkedHashSet<>();
        secondTags.add("book");
        secondTags.add("daily");
        secondTags.add("DAILY");

        String first = hash(new QuickRecordCommand.Create(
                " collection ",
                new CollectionCommand.Create(
                        " book ",
                        "  title  ",
                        "   ",
                        1,
                        " condition ",
                        " source ",
                        firstTags
                ),
                null,
                null
        ));
        String second = hash(new QuickRecordCommand.Create(
                "COLLECTION",
                new CollectionCommand.Create(
                        "BOOK",
                        "title",
                        null,
                        1,
                        "condition",
                        "source",
                        secondTags
                ),
                null,
                null
        ));

        assertThat(first)
                .hasSize(64)
                .matches("[0-9a-f]{64}")
                .isEqualTo(second);
    }

    @Test
    @DisplayName("선택된 subtype payload 전체가 hash에 반영된다")
    void hashesAllSelectedPayloadFields() {
        String collection = hash(new QuickRecordCommand.Create(
                "COLLECTION",
                collection("title", Set.of("tag")),
                null,
                null
        ));
        String changedCollection = hash(new QuickRecordCommand.Create(
                "COLLECTION",
                collection("changed", Set.of("tag")),
                null,
                null
        ));
        String exercise = hash(new QuickRecordCommand.Create(
                "EXERCISE", null, exercise(), null
        ));
        String media = hash(new QuickRecordCommand.Create(
                "MEDIA", null, null, media()
        ));

        assertThat(Set.of(
                collection,
                changedCollection,
                exercise,
                media
        )).hasSize(4);
    }

    @Test
    @DisplayName("길이 prefix가 필드 경계와 구분자 충돌을 방지한다")
    void preventsFieldBoundaryCollisions() {
        String first = hash(new QuickRecordCommand.Create(
                "COLLECTION",
                collection("a;4:tags=text:1:b", Set.of("c")),
                null,
                null
        ));
        String second = hash(new QuickRecordCommand.Create(
                "COLLECTION",
                collection("a", Set.of("b;4:tags=text:1:c")),
                null,
                null
        ));

        assertThat(first).isNotEqualTo(second);
    }

    private String hash(QuickRecordCommand.Create command) {
        return hasher.hash(command.selected());
    }

    private void assertInvalid(QuickRecordCommand.Create command) {
        assertThatThrownBy(command::selected)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(QuickRecordError.INVALID_REQUEST)
                );
    }

    private CollectionCommand.Create collection(
            String title,
            Set<String> tags
    ) {
        return new CollectionCommand.Create(
                "BOOK",
                title,
                null,
                1,
                null,
                null,
                tags
        );
    }

    private ExerciseCommand.Create exercise() {
        return new ExerciseCommand.Create(
                "RUNNING",
                30,
                5.0,
                250,
                LocalDate.of(2026, 7, 24),
                "memo"
        );
    }

    private MediaLogCommand.Create media() {
        return new MediaLogCommand.Create(
                "MOVIE",
                "title",
                null,
                0,
                1,
                "PLANNED",
                Set.of("tag")
        );
    }
}
