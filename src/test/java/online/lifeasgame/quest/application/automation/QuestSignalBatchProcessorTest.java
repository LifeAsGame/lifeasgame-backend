package online.lifeasgame.quest.application.automation;

import online.lifeasgame.quest.domain.QuestCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestSignalBatchProcessor")
class QuestSignalBatchProcessorTest {

    @Mock
    private QuestSignalProcessingService processingService;

    private QuestSignalBatchProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new QuestSignalBatchProcessor(processingService);
    }

    @Nested
    @DisplayName("Signal Collection을 처리할 때")
    class ProcessSignals {

        @Test
        @DisplayName("각 Signal을 순서대로 독립 Processing Service에 위임한다")
        void delegatesEachSignalInOrder() {
            QuestSignal first = signal("source:first", 1);
            QuestSignal second = signal("source:second", 2);
            QuestSignalProcessingResult firstResult =
                    QuestSignalProcessingResult.applied(1L);
            QuestSignalProcessingResult secondResult =
                    QuestSignalProcessingResult.replayed(2L);
            given(processingService.process(first)).willReturn(firstResult);
            given(processingService.process(second)).willReturn(secondResult);

            List<QuestSignalProcessingResult> results =
                    processor.process(List.of(first, second));

            assertThat(results).containsExactly(firstResult, secondResult);
            InOrder inOrder = inOrder(processingService);
            inOrder.verify(processingService).process(first);
            inOrder.verify(processingService).process(second);
        }

        @Test
        @DisplayName("null 또는 빈 Collection은 처리하지 않는다")
        void ignoresEmptySignals() {
            assertThat(processor.process(null)).isEmpty();
            assertThat(processor.process(List.of())).isEmpty();

            verifyNoInteractions(processingService);
        }
    }

    private QuestSignal signal(String correlationId, int delta) {
        return QuestSignal.addProgress(
                        QuestCode.COLLECTION_HUNTER_10,
                        195L,
                        delta
                )
                .occurredAt(Instant.parse("2026-07-24T01:00:00Z"))
                .correlationId(correlationId)
                .build();
    }
}
