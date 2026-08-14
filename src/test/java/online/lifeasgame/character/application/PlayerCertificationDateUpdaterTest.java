package online.lifeasgame.character.application;

import java.time.LocalDate;
import java.util.Optional;
import online.lifeasgame.character.domain.PlayerCertification;
import online.lifeasgame.character.domain.error.PlayerCertificationError;
import online.lifeasgame.character.domain.repository.PlayerCertificationRepository;
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
@DisplayName("Player certification date updater")
class PlayerCertificationDateUpdaterTest {

    private static final Long PLAYER_ID = 272L;
    private static final Long CERTIFICATION_ID = 27201L;
    private static final LocalDate ACQUIRED_DATE = LocalDate.of(2025, 1, 10);
    private static final LocalDate EXPIRES_DATE = LocalDate.of(2026, 1, 10);

    @Mock
    private PlayerCertificationRepository repository;

    @Nested
    @DisplayName("자격증 날짜를 부분 수정할 때")
    class PartialDateUpdate {

        @Test
        @DisplayName("취득일만 전달하면 만료일은 기존 값을 유지한다")
        void changesOnlyAcquiredDate() {
            PlayerCertification certification = certification();
            givenOwnedCertification(certification);
            LocalDate changedAcquiredDate = LocalDate.of(2025, 2, 10);

            updater().update(PLAYER_ID, CERTIFICATION_ID, changedAcquiredDate, null);

            assertThat(certification.getAcquiredDate()).isEqualTo(changedAcquiredDate);
            assertThat(certification.getExpiresDate()).isEqualTo(EXPIRES_DATE);
        }

        @Test
        @DisplayName("만료일만 전달하면 취득일은 기존 값을 유지한다")
        void changesOnlyExpiresDate() {
            PlayerCertification certification = certification();
            givenOwnedCertification(certification);
            LocalDate changedExpiresDate = LocalDate.of(2026, 2, 10);

            updater().update(PLAYER_ID, CERTIFICATION_ID, null, changedExpiresDate);

            assertThat(certification.getAcquiredDate()).isEqualTo(ACQUIRED_DATE);
            assertThat(certification.getExpiresDate()).isEqualTo(changedExpiresDate);
        }

        @Test
        @DisplayName("두 날짜가 모두 null이면 기존 값을 유지한다")
        void preservesBothDates() {
            PlayerCertification certification = certification();
            givenOwnedCertification(certification);

            updater().update(PLAYER_ID, CERTIFICATION_ID, null, null);

            assertThat(certification.getAcquiredDate()).isEqualTo(ACQUIRED_DATE);
            assertThat(certification.getExpiresDate()).isEqualTo(EXPIRES_DATE);
        }

        @Test
        @DisplayName("최종 날짜 순서가 잘못되면 기존 도메인 오류를 반환한다")
        void rejectsInvalidEffectiveDateOrder() {
            PlayerCertification certification = certification();
            givenOwnedCertification(certification);
            LocalDate invalidExpiresDate = ACQUIRED_DATE.minusDays(1);

            assertThatThrownBy(() -> updater().update(
                    PLAYER_ID,
                    CERTIFICATION_ID,
                    null,
                    invalidExpiresDate
            )).isInstanceOfSatisfying(DomainException.class, exception ->
                    assertThat(exception.getErrorCode())
                            .isEqualTo(PlayerCertificationError.EXPIRES_BEFORE_ACQUIRED)
            );
            assertThat(certification.getAcquiredDate()).isEqualTo(ACQUIRED_DATE);
            assertThat(certification.getExpiresDate()).isEqualTo(EXPIRES_DATE);
        }
    }

    private PlayerCertificationDateUpdater updater() {
        return new PlayerCertificationDateUpdater(repository);
    }

    private PlayerCertification certification() {
        return PlayerCertification.create(PLAYER_ID, CERTIFICATION_ID, ACQUIRED_DATE, EXPIRES_DATE);
    }

    private void givenOwnedCertification(PlayerCertification certification) {
        given(repository.findByPlayerIdAndCertificationId(PLAYER_ID, CERTIFICATION_ID))
                .willReturn(Optional.of(certification));
    }
}
