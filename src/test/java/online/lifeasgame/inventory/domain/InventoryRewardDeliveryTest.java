package online.lifeasgame.inventory.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.domain.error.InventoryError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InventoryRewardDelivery")
class InventoryRewardDeliveryTest {

    private static final Instant DELIVERED_AT =
            Instant.parse("2026-08-03T08:00:00Z");

    @Test
    @DisplayName("양수 identity와 정규화 ItemCode로 append-only receipt를 생성한다")
    void createsReceipt() {
        InventoryRewardDelivery delivery = delivery();

        assertThat(delivery.getRewardLineId()).isEqualTo(101L);
        assertThat(delivery.getPlayerId()).isEqualTo(201L);
        assertThat(delivery.getItemCode()).isEqualTo("IT_FIRST_STEP_FRAGMENT");
        assertThat(delivery.getItemId()).isEqualTo(301L);
        assertThat(delivery.getQuantity()).isEqualTo(2L);
        assertThat(delivery.getDeliveredAt()).isEqualTo(DELIVERED_AT);
    }

    @Test
    @DisplayName("동일 payload replay는 일치한다")
    void acceptsMatchingPayload() {
        assertThatCode(() -> delivery().assertMatches(
                101L,
                201L,
                ItemCode.of("IT_FIRST_STEP_FRAGMENT"),
                2L
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("같은 rewardLineId의 다른 payload는 stable 409 conflict다")
    void rejectsMismatchedPayload() {
        InventoryRewardDelivery delivery = delivery();

        assertConflict(() -> delivery.assertMatches(
                101L,
                202L,
                ItemCode.of("IT_FIRST_STEP_FRAGMENT"),
                2L
        ));
        assertConflict(() -> delivery.assertMatches(
                101L,
                201L,
                ItemCode.of("IT_OTHER"),
                2L
        ));
        assertConflict(() -> delivery.assertMatches(
                101L,
                201L,
                ItemCode.of("IT_FIRST_STEP_FRAGMENT"),
                3L
        ));
    }

    @Test
    @DisplayName("유효하지 않은 receipt identity와 quantity를 거부한다")
    void rejectsInvalidReceipt() {
        assertError(
                () -> InventoryRewardDelivery.create(
                        0L,
                        201L,
                        ItemCode.of("IT_FIRST_STEP_FRAGMENT"),
                        301L,
                        2L,
                        DELIVERED_AT
                ),
                InventoryError.REWARD_LINE_ID_INVALID
        );
        assertError(
                () -> InventoryRewardDelivery.create(
                        101L,
                        0L,
                        ItemCode.of("IT_FIRST_STEP_FRAGMENT"),
                        301L,
                        2L,
                        DELIVERED_AT
                ),
                InventoryError.PLAYER_ID_INVALID
        );
        assertError(
                () -> InventoryRewardDelivery.create(
                        101L,
                        201L,
                        ItemCode.of("IT_FIRST_STEP_FRAGMENT"),
                        301L,
                        0L,
                        DELIVERED_AT
                ),
                InventoryError.REWARD_QUANTITY_INVALID
        );
    }

    private InventoryRewardDelivery delivery() {
        return InventoryRewardDelivery.create(
                101L,
                201L,
                ItemCode.of("  IT_FIRST_STEP_FRAGMENT  "),
                301L,
                2L,
                DELIVERED_AT
        );
    }

    private void assertConflict(Runnable assertion) {
        assertError(assertion, InventoryError.REWARD_DELIVERY_CONFLICT);
    }

    private void assertError(Runnable assertion, InventoryError error) {
        assertThatThrownBy(assertion::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(error)
                );
    }
}
