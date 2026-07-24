package online.lifeasgame.quest.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuestStatus")
class QuestStatusTest {

    @Nested
    @DisplayName("Legacy DONE 문자열을 파싱할 때")
    class ParseLegacyDone {

        @Test
        @DisplayName("단건 입력은 COMPLETED로 해석한다")
        void parsesSingleValueAsCompleted() {
            assertThat(QuestStatus.parse(" done ")).isEqualTo(QuestStatus.COMPLETED);
        }

        @Test
        @DisplayName("목록 입력도 COMPLETED로 해석하고 중복을 제거한다")
        void parsesListAsCompleted() {
            assertThat(QuestStatus.parse(List.of("DONE", "COMPLETED")))
                    .containsExactly(QuestStatus.COMPLETED);
        }

        @Test
        @DisplayName("Enum과 신규 저장 상태에는 DONE이 존재하지 않는다")
        void excludesDoneFromEnumContract() {
            assertThat(QuestStatus.values())
                    .containsExactly(
                            QuestStatus.IN_PROGRESS,
                            QuestStatus.GOAL_REACHED,
                            QuestStatus.COMPLETED,
                            QuestStatus.CANCELED
                    );
        }
    }
}
