package online.lifeasgame.character.application;

import online.lifeasgame.character.domain.PlayerEquipment;
import online.lifeasgame.character.domain.error.PlayerEquipmentError;
import online.lifeasgame.character.domain.repository.PlayerEquipmentRepository;
import online.lifeasgame.core.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("Player equipment writer")
class PlayerEquipmentWriterTest {

    private static final Long PLAYER_ID = 262L;
    private static final Long SLOT_ID = 1L;
    private static final Long ITEM_INSTANCE_ID = 26201L;

    @Mock
    private PlayerEquipmentRepository repository;

    @Nested
    @DisplayName("DB unique conflict가 발생하면")
    class UniqueConflict {

        @Test
        @DisplayName("equipped-item constraint만 stable semantic conflict로 매핑한다")
        void mapsOnlyEquippedItemConstraint() {
            PlayerEquipment equipment = emptyEquipment();
            DataIntegrityViolationException conflict =
                    new DataIntegrityViolationException(
                            "Duplicate entry for key 'uq_player_equipment_item'"
                    );
            given(repository.findByPlayerIdAndSlotIdForUpdate(
                    PLAYER_ID,
                    SLOT_ID
            )).willReturn(Optional.of(equipment));
            given(repository.saveAndFlush(equipment)).willThrow(conflict);

            assertThatThrownBy(() -> writer().equip(
                    PLAYER_ID,
                    SLOT_ID,
                    ITEM_INSTANCE_ID
            )).isInstanceOfSatisfying(DomainException.class, exception -> {
                assertThat(exception.getErrorCode()).isEqualTo(
                        PlayerEquipmentError.ALREADY_EQUIPPED_ITEM
                );
                assertThat(exception.getCause()).isSameAs(conflict);
            });
        }

        @Test
        @DisplayName("다른 constraint violation은 원래 예외를 유지한다")
        void preservesUnrelatedConstraintFailure() {
            PlayerEquipment equipment = emptyEquipment();
            DataIntegrityViolationException unrelated =
                    new DataIntegrityViolationException(
                            "Duplicate entry for key 'uq_player_slot'"
                    );
            given(repository.findByPlayerIdAndSlotIdForUpdate(
                    PLAYER_ID,
                    SLOT_ID
            )).willReturn(Optional.of(equipment));
            given(repository.saveAndFlush(equipment)).willThrow(unrelated);

            assertThatThrownBy(() -> writer().equip(
                    PLAYER_ID,
                    SLOT_ID,
                    ITEM_INSTANCE_ID
            )).isSameAs(unrelated);
        }
    }

    private PlayerEquipmentWriter writer() {
        return new PlayerEquipmentWriter(repository);
    }

    private PlayerEquipment emptyEquipment() {
        return PlayerEquipment.create(PLAYER_ID, SLOT_ID, null);
    }
}
