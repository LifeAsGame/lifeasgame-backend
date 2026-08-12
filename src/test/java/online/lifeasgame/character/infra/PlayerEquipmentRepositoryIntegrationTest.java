package online.lifeasgame.character.infra;

import online.lifeasgame.character.domain.PlayerEquipment;
import online.lifeasgame.character.domain.repository.PlayerEquipmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Player equipment repository invariants")
class PlayerEquipmentRepositoryIntegrationTest {

    private static final Long PLAYER_ID = 262L;
    private static final Long ITEM_INSTANCE_ID = 26201L;

    @Autowired
    private PlayerEquipmentRepository repository;

    @Nested
    @DisplayName("장착 item을 조회할 때")
    class FindEquippedItem {

        @Test
        @DisplayName("slot과 무관하게 같은 Player 전체에서 찾고 다른 Player와 격리한다")
        void checksPlayerWideItemIdentity() {
            repository.saveAndFlush(PlayerEquipment.create(
                    PLAYER_ID,
                    1L,
                    ITEM_INSTANCE_ID
            ));

            assertThat(repository.existsByPlayerIdAndItemInstanceId(
                    PLAYER_ID,
                    ITEM_INSTANCE_ID
            )).isTrue();
            assertThat(repository.existsByPlayerIdAndItemInstanceId(
                    PLAYER_ID + 1,
                    ITEM_INSTANCE_ID
            )).isFalse();
        }
    }

    @Nested
    @DisplayName("장착 상태를 저장할 때")
    class PersistEquipment {

        @Test
        @DisplayName("같은 Player의 동일 item은 다른 slot에도 중복 저장할 수 없다")
        void rejectsDuplicateItemAcrossSlots() {
            repository.saveAndFlush(PlayerEquipment.create(
                    PLAYER_ID,
                    1L,
                    ITEM_INSTANCE_ID
            ));

            assertThatThrownBy(() -> repository.saveAndFlush(
                    PlayerEquipment.create(
                            PLAYER_ID,
                            2L,
                            ITEM_INSTANCE_ID
                    )
            )).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("nullable empty slot과 다른 Player의 동일 item은 허용한다")
        void preservesNullableAndPlayerScopedSemantics() {
            repository.saveAndFlush(PlayerEquipment.create(
                    PLAYER_ID,
                    1L,
                    null
            ));
            assertThatCode(() -> repository.saveAndFlush(
                    PlayerEquipment.create(PLAYER_ID, 2L, null)
            )).doesNotThrowAnyException();
            assertThatCode(() -> repository.saveAndFlush(
                    PlayerEquipment.create(
                            PLAYER_ID + 1,
                            1L,
                            ITEM_INSTANCE_ID
                    )
            )).doesNotThrowAnyException();
            assertThatCode(() -> repository.saveAndFlush(
                    PlayerEquipment.create(
                            PLAYER_ID + 2,
                            1L,
                            ITEM_INSTANCE_ID
                    )
            )).doesNotThrowAnyException();
        }
    }
}
