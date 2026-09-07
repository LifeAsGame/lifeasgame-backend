package online.lifeasgame.character.application;

import online.lifeasgame.character.domain.EquipmentSlot;
import online.lifeasgame.character.domain.EquipmentSlotCategory;
import online.lifeasgame.character.domain.error.EquipmentSlotError;
import online.lifeasgame.core.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("Equipment slot read")
class EquipmentSlotServiceTest {

    @Mock
    private EquipmentSlotReader reader;

    @Nested
    @DisplayName("command-supported slot 목록을 조회할 때")
    class GetEquipmentSlots {

        @Test
        @DisplayName("sortOrder가 없으면 authority conflict로 거부한다")
        void rejectsMissingSortOrder() {
            EquipmentSlotService service = new EquipmentSlotService(reader);
            given(reader.getByCategoriesAndRoles(List.of(), List.of()))
                    .willReturn(List.of(slot(1L, "HEAD", null)));

            assertThatThrownBy(() -> service.getEquipmentSlots(
                    List.of(),
                    List.of()
            )).isInstanceOfSatisfying(DomainException.class, exception ->
                    assertThat(exception.getErrorCode()).isEqualTo(
                            EquipmentSlotError
                                    .EQUIPMENT_SLOT_AUTHORITY_CONFLICT
                    )
            );
        }

        @Test
        @DisplayName("유효한 slot은 sortOrder 순으로 반환한다")
        void sortsBySortOrder() {
            EquipmentSlotService service = new EquipmentSlotService(reader);
            given(reader.getByCategoriesAndRoles(List.of(), List.of()))
                    .willReturn(List.of(
                            slot(2L, "BODY", 20),
                            slot(1L, "HEAD", 10)
                    ));

            assertThat(service.getEquipmentSlots(List.of(), List.of()))
                    .extracting(result -> result.code())
                    .containsExactly("HEAD", "BODY");
        }
    }

    private EquipmentSlot slot(Long id, String code, Integer sortOrder) {
        EquipmentSlot slot = EquipmentSlot.of(
                code,
                code,
                EquipmentSlotCategory.HEAD
        );
        ReflectionTestUtils.setField(slot, "id", id);
        ReflectionTestUtils.setField(slot, "sortOrder", sortOrder);
        return slot;
    }
}
