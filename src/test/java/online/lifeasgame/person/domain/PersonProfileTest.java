package online.lifeasgame.person.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.person.domain.error.PersonError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonProfileTest {
    @Test
    @DisplayName("선택 태그를 정리하고 생략한 값은 빈 배열로 돌려준다")
    void normalizes() {
        PersonProfile profile = PersonProfile.normalized(new PersonProfile(
                "  별명  ", null, 23, LocalDate.of(2026, 9, 30),
                null, null, null, null, null,
                List.of("  산책  ", "", "산책"), null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null
        ));

        assertThat(profile.nickname()).isEqualTo("별명");
        assertThat(profile.hobbies()).containsExactly("산책");
        assertThat(profile.interests()).isEmpty();
        assertThat(PersonProfile.fromJson(profile.toJson())).isEqualTo(profile);
    }

    @Test
    @DisplayName("기록 나이와 날짜가 짝을 이루지 않으면 거부한다")
    void rejectsIncompleteAge() {
        assertThatThrownBy(() -> PersonProfile.normalized(new PersonProfile(
                null, null, 25, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null
        ))).isInstanceOfSatisfying(DomainException.class, error ->
                assertThat(error.getErrorCode()).isEqualTo(PersonError.INVALID_PERSON_PROFILE));
    }

    @Test
    @DisplayName("정규화한 JSON이 64KiB를 넘으면 거부한다")
    void rejectsOversizeUtf8() {
        String longKorean = "가".repeat(100);
        List<String> tags = java.util.stream.IntStream.range(0, 20)
                .mapToObj(i -> longKorean.substring(0, 97) + String.format("%03d", i)).toList();
        assertThatThrownBy(() -> PersonProfile.normalized(new PersonProfile(
                null, null, null, null, null, null, null, null, null,
                tags, tags, tags, tags, tags, tags, tags, tags, tags, tags, tags, tags,
                null, null, null, null, null, null
        ))).isInstanceOf(DomainException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"nickname\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"}",
            "{\"ageAtReference\":151,\"ageReferenceDate\":\"2026-09-30\"}",
            "{\"contactChannels\":[{\"kind\":\"EMAIL\",\"value\":\" \"}]}",
            "{\"importantDates\":[{\"label\":\"Exam\",\"date\":\"2026-10-01\"}]}"
    })
    @DisplayName("필드 길이와 필수 내부 값을 벗어나면 저장 전에 거부한다")
    void rejectsInvalidFields(String json) {
        assertThatThrownBy(() -> PersonProfile.fromJson(json))
                .isInstanceOfSatisfying(DomainException.class, error ->
                        assertThat(error.getErrorCode()).isEqualTo(PersonError.INVALID_PERSON_PROFILE));
    }
}
