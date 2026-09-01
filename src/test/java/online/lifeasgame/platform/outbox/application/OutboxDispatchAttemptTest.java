package online.lifeasgame.platform.outbox.application;

import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.platform.outbox.application.codec.OutboxEventCodecRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
@DisplayName("Outbox dispatch attempt")
class OutboxDispatchAttemptTest {

    private static final String EVENT_ID =
            "31700000-0000-0000-0000-000000000001";

    @Mock
    private OutboxEventCodecRegistry codecRegistry;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private DomainEvent event;

    private OutboxDispatchAttempt dispatchAttempt;

    @BeforeEach
    void setUp() {
        dispatchAttempt = new OutboxDispatchAttempt(
                codecRegistry,
                new LocalDomainEventDispatcher(applicationEventPublisher)
        );
    }

    @Test
    @DisplayName("claim의 canonical event id를 delivery envelope에 그대로 보존한다")
    void retainsCanonicalEventId() {
        OutboxClaim claim = claim();
        given(codecRegistry.decode(claim.eventType(), claim.payload()))
                .willReturn(event);

        dispatchAttempt.dispatch(claim);

        InOrder events = inOrder(applicationEventPublisher);
        events.verify(applicationEventPublisher).publishEvent(event);
        events.verify(applicationEventPublisher).publishEvent(
                new OutboxEventDelivery(EVENT_ID, event)
        );
    }

    @Test
    @DisplayName("delivery consumer 실패를 삼키지 않고 outbox relay에 전달한다")
    void propagatesConsumerFailure() {
        OutboxClaim claim = claim();
        RuntimeException failure = new IllegalStateException("append failed");
        given(codecRegistry.decode(claim.eventType(), claim.payload()))
                .willReturn(event);
        willAnswer(invocation -> {
            if (invocation.getArgument(0) instanceof OutboxEventDelivery) {
                throw failure;
            }
            return null;
        }).given(applicationEventPublisher).publishEvent(any(Object.class));

        assertThatThrownBy(() -> dispatchAttempt.dispatch(claim))
                .isSameAs(failure);
    }

    private OutboxClaim claim() {
        return new OutboxClaim(
                317L,
                EVENT_ID,
                "quest.event.v1",
                "{}",
                "test-instance"
        );
    }
}
