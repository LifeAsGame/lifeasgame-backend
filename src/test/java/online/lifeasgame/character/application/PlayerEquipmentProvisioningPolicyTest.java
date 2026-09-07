package online.lifeasgame.character.application;

import online.lifeasgame.character.domain.EquipmentSlot;
import online.lifeasgame.character.domain.EquipmentSlotLifecycleStatus;
import online.lifeasgame.character.domain.error.EquipmentSlotError;
import online.lifeasgame.core.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("PlayerEquipment stable provisioning policy")
class PlayerEquipmentProvisioningPolicyTest {

    private final AtomicLong ids = new AtomicLong(900L);

    @Nested
    @DisplayName("LAG-EQSA 1.0.0 catalog를 해석할 때")
    class ResolveCatalog {

        @Test
        @DisplayName("ID와 입력 순서에 무관하게 정확한 9개 eager code를 선택한다")
        void resolvesStableCodesIndependentlyOfIdsAndOrder() {
            List<EquipmentSlot> catalog = validCatalog();
            catalog.add(slot(
                    "TITLE",
                    "1.0.0",
                    true,
                    EquipmentSlotLifecycleStatus.ACTIVE,
                    false
            ));
            catalog.add(slot(
                    "FACE",
                    "1.0.0",
                    false,
                    EquipmentSlotLifecycleStatus.GATED,
                    false
            ));
            Collections.reverse(catalog);

            assertThat(PlayerEquipmentProvisioningPolicy
                    .resolveRequiredDefinitions(catalog))
                    .extracting(EquipmentSlot::getCode)
                    .containsExactlyElementsOf(
                            PlayerEquipmentProvisioningPolicy.REQUIRED_CODES
                    )
                    .doesNotContain("TITLE", "FACE");
        }

        @Test
        @DisplayName("필수 code가 없거나 승인 version이 아니면 fail closed한다")
        void rejectsMissingAndWrongVersion() {
            List<EquipmentSlot> missing = validCatalog();
            missing.removeFirst();
            assertAuthorityConflict(missing);

            List<EquipmentSlot> wrongVersion = validCatalog();
            wrongVersion.set(0, slot(
                    "HEAD",
                    "0.9.0",
                    true,
                    EquipmentSlotLifecycleStatus.ACTIVE,
                    true
            ));
            assertAuthorityConflict(wrongVersion);
        }

        @Test
        @DisplayName("필수 정의가 중복되면 fail closed한다")
        void rejectsAmbiguousDefinition() {
            List<EquipmentSlot> ambiguous = validCatalog();
            ambiguous.add(slot(
                    "HEAD",
                    "1.0.0",
                    true,
                    EquipmentSlotLifecycleStatus.ACTIVE,
                    true
            ));

            assertAuthorityConflict(ambiguous);
        }

        @Test
        @DisplayName("필수 정의가 disabled, non-ACTIVE, non-eager면 fail closed한다")
        void rejectsNonProvisionableDefinition() {
            for (EquipmentSlot invalid : List.of(
                    slot(
                            "HEAD",
                            "1.0.0",
                            false,
                            EquipmentSlotLifecycleStatus.ACTIVE,
                            true
                    ),
                    slot(
                            "HEAD",
                            "1.0.0",
                            true,
                            EquipmentSlotLifecycleStatus.GATED,
                            true
                    ),
                    slot(
                            "HEAD",
                            "1.0.0",
                            true,
                            EquipmentSlotLifecycleStatus.ACTIVE,
                            false
                    )
            )) {
                List<EquipmentSlot> catalog = validCatalog();
                catalog.set(0, invalid);
                assertAuthorityConflict(catalog);
            }
        }
    }

    private List<EquipmentSlot> validCatalog() {
        List<EquipmentSlot> result = new ArrayList<>();
        for (String code : PlayerEquipmentProvisioningPolicy.REQUIRED_CODES) {
            result.add(slot(
                    code,
                    "1.0.0",
                    true,
                    EquipmentSlotLifecycleStatus.ACTIVE,
                    true
            ));
        }
        return result;
    }

    private EquipmentSlot slot(
            String code,
            String version,
            boolean enabled,
            EquipmentSlotLifecycleStatus lifecycle,
            boolean eager
    ) {
        EquipmentSlot slot = mock(EquipmentSlot.class);
        given(slot.getId()).willReturn(ids.incrementAndGet());
        given(slot.getCode()).willReturn(code);
        given(slot.getDefinitionVersion()).willReturn(version);
        given(slot.isEnabled()).willReturn(enabled);
        given(slot.getLifecycleStatus()).willReturn(lifecycle);
        given(slot.isEagerOnLinkStart()).willReturn(eager);
        return slot;
    }

    private void assertAuthorityConflict(List<EquipmentSlot> catalog) {
        assertThatThrownBy(() -> PlayerEquipmentProvisioningPolicy
                .resolveRequiredDefinitions(catalog))
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                EquipmentSlotError
                                        .EQUIPMENT_SLOT_AUTHORITY_CONFLICT
                        )
                );
    }
}
