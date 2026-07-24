package online.lifeasgame.quest.application.automation;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.quest.domain.error.QuestError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("QuestSignal")
class QuestSignalTest {

    @Nested
    @DisplayName("correlationId를 검증할 때")
    class ValidateCorrelation {

        @Test
        @DisplayName("null과 blank correlation은 거부한다")
        void rejectsMissingCorrelation() {
            assertQuestError(
                    () -> signal(null),
                    QuestError.QUEST_SIGNAL_CORRELATION_REQUIRED
            );
            assertQuestError(
                    () -> signal("   "),
                    QuestError.QUEST_SIGNAL_CORRELATION_REQUIRED
            );
        }

        @Test
        @DisplayName("최대 길이를 넘는 correlation은 거부한다")
        void rejectsTooLongCorrelation() {
            assertQuestError(
                    () -> signal(
                            "a".repeat(
                                    QuestSignal.MAX_CORRELATION_ID_LENGTH + 1
                            )
                    ),
                    QuestError.QUEST_SIGNAL_CORRELATION_TOO_LONG
            );
        }

        @Test
        @DisplayName("유효한 correlation은 trim해서 보관한다")
        void trimsCorrelation() {
            QuestSignal signal = signal("  source:event:195  ");

            assertThat(signal.correlationId())
                    .isEqualTo("source:event:195");
        }
    }

    private QuestSignal signal(String correlationId) {
        return QuestSignal.addProgress(
                        QuestCode.COLLECTION_HUNTER_10,
                        195L,
                        1
                )
                .correlationId(correlationId)
                .build();
    }

    private void assertQuestError(Runnable action, QuestError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(error)
                );
    }
}
