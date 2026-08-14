package online.lifeasgame.character.application;

import java.time.LocalDate;
import java.util.Optional;
import online.lifeasgame.character.application.command.PlayerHobbyCommand;
import online.lifeasgame.character.domain.PlayerHobby;
import online.lifeasgame.character.domain.PlayerHobbyStatus;
import online.lifeasgame.character.domain.error.PlayerHobbyError;
import online.lifeasgame.character.domain.repository.PlayerHobbyRepository;
import online.lifeasgame.core.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerHobbyUpdater")
class PlayerHobbyUpdaterTest {

    private static final Long PLAYER_ID = 274L;
    private static final Long HOBBY_ID = 27401L;
    private static final LocalDate STARTED_ON = LocalDate.of(2025, 1, 10);

    @Mock
    private PlayerHobbyRepository repository;

    @Nested
    @DisplayName("보유 취미를 부분 수정할 때")
    class PartialUpdate {

        @Test
        @DisplayName("customName만 바꾸고 null proficiency/status를 포함한 생략 값은 보존한다")
        void changesSubsetAndPreservesOmittedValues() {
            PlayerHobby playerHobby = playerHobby();
            givenOwnedHobby(playerHobby);

            updater().update(
                    PLAYER_ID,
                    new PlayerHobbyCommand.Change(
                            HOBBY_ID,
                            "변경 이름",
                            null,
                            null,
                            null,
                            null
                    )
            );

            assertThat(playerHobby.getCustomName()).isEqualTo("변경 이름");
            assertThat(playerHobby.getDetail()).isEqualTo("기존 상세");
            assertThat(playerHobby.getProficiency()).isEqualTo(60);
            assertThat(playerHobby.getStatus()).isEqualTo(PlayerHobbyStatus.ACTIVE);
            assertThat(playerHobby.getStartedOn()).isEqualTo(STARTED_ON);
        }

        @Test
        @DisplayName("유효하지 않은 supplied status는 기존 domain error로 거부한다")
        void rejectsInvalidSuppliedStatus() {
            PlayerHobby playerHobby = playerHobby();
            givenOwnedHobby(playerHobby);

            assertThatThrownBy(() -> updater().update(
                    PLAYER_ID,
                    new PlayerHobbyCommand.Change(
                            HOBBY_ID,
                            null,
                            null,
                            null,
                            "INVALID",
                            null
                    )
            )).isInstanceOfSatisfying(
                    DomainException.class,
                    exception -> assertThat(exception.getErrorCode())
                            .isEqualTo(PlayerHobbyError.INVALID_PLAYER_HOBBY_STATUS)
            );
            assertThat(playerHobby.getStatus()).isEqualTo(PlayerHobbyStatus.ACTIVE);
        }

        @Test
        @DisplayName("범위를 벗어난 supplied proficiency는 aggregate 변경 전에 거부한다")
        void rejectsInvalidSuppliedProficiencyBeforeMutation() {
            PlayerHobby playerHobby = playerHobby();
            givenOwnedHobby(playerHobby);

            assertThatThrownBy(() -> updater().update(
                    PLAYER_ID,
                    new PlayerHobbyCommand.Change(
                            HOBBY_ID,
                            "변경되면 안 됨",
                            null,
                            101,
                            null,
                            null
                    )
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("proficiency must be between 0 and 100");
            assertThat(playerHobby.getCustomName()).isEqualTo("기존 이름");
            assertThat(playerHobby.getProficiency()).isEqualTo(60);
        }
    }

    private PlayerHobbyUpdater updater() {
        return new PlayerHobbyUpdater(repository);
    }

    private PlayerHobby playerHobby() {
        return PlayerHobby.create(
                PLAYER_ID,
                HOBBY_ID,
                "기존 이름",
                "기존 상세",
                60,
                PlayerHobbyStatus.ACTIVE,
                STARTED_ON
        );
    }

    private void givenOwnedHobby(PlayerHobby playerHobby) {
        given(repository.findByPlayerIdAndHobbyId(PLAYER_ID, HOBBY_ID))
                .willReturn(Optional.of(playerHobby));
    }
}
