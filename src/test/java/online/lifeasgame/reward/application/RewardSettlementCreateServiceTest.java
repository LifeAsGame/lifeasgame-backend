package online.lifeasgame.reward.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.reward.domain.RewardDefinition;
import online.lifeasgame.reward.domain.RewardProfile;
import online.lifeasgame.reward.domain.RewardProfileStatus;
import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.RewardSettlementSourceType;
import online.lifeasgame.reward.domain.RewardType;
import online.lifeasgame.reward.domain.error.RewardError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RewardSettlementCreateService")
class RewardSettlementCreateServiceTest {

    private static final Long PLAYER_ID = 1L;
    private static final Long SOURCE_ID = 1000L;
    private static final String PROFILE_CODE = "RP_EXP_10";

    @Mock
    private RewardSettlementReader settlementReader;

    @Mock
    private RewardProfileReader profileReader;

    @Mock
    private RewardSettlementWriter settlementWriter;

    private RewardSettlementCreateService service;

    @BeforeEach
    void setUp() {
        service = new RewardSettlementCreateService(settlementReader, profileReader, settlementWriter);
    }

    @Nested
    @DisplayName("활성 RewardProfile로 Settlement를 생성할 때")
    class CreateNewSettlement {

        @Test
        @DisplayName("Profile Line 스냅샷을 한 번 저장한다")
        void savesLineSnapshotsOnce() {
            RewardProfile profile = activeProfile();
            given(settlementReader.findByIdentity(PLAYER_ID, sourceType(), SOURCE_ID))
                    .willReturn(Optional.empty());
            given(profileReader.getActiveByCodeOrThrow(PROFILE_CODE)).willReturn(profile);
            given(settlementWriter.saveAndFlush(any(RewardSettlement.class)))
                    .willAnswer(invocation -> invocation.getArgument(0, RewardSettlement.class));

            RewardSettlement result = create();

            assertThat(result.getLines()).hasSize(1);
            assertThat(result.getLines().getFirst().getAmount()).isEqualTo(10L);
            verify(settlementWriter).saveAndFlush(result);
        }

        @Test
        @DisplayName("비활성 Profile이면 REWARD_PROFILE_INACTIVE 예외가 발생한다")
        void rejectsInactiveProfile() {
            given(settlementReader.findByIdentity(PLAYER_ID, sourceType(), SOURCE_ID))
                    .willReturn(Optional.empty());
            given(profileReader.getActiveByCodeOrThrow(PROFILE_CODE))
                    .willThrow(new DomainException(RewardError.REWARD_PROFILE_INACTIVE));

            assertRewardError(this::createSettlement, RewardError.REWARD_PROFILE_INACTIVE);
            verify(settlementWriter, never()).saveAndFlush(any(RewardSettlement.class));
        }

        @Test
        @DisplayName("Profile이 없으면 REWARD_PROFILE_NOT_FOUND 예외가 발생한다")
        void rejectsMissingProfile() {
            given(settlementReader.findByIdentity(PLAYER_ID, sourceType(), SOURCE_ID))
                    .willReturn(Optional.empty());
            given(profileReader.getActiveByCodeOrThrow(PROFILE_CODE))
                    .willThrow(new DomainException(RewardError.REWARD_PROFILE_NOT_FOUND));

            assertRewardError(this::createSettlement, RewardError.REWARD_PROFILE_NOT_FOUND);
            verify(settlementWriter, never()).saveAndFlush(any(RewardSettlement.class));
        }

        private void createSettlement() {
            create();
        }
    }

    @Nested
    @DisplayName("같은 정산 식별자로 다시 생성할 때")
    class CreateExistingSettlement {

        @Test
        @DisplayName("새 Line을 만들지 않고 기존 Settlement를 반환한다")
        void returnsExistingSettlement() {
            RewardSettlement existing = settlement(activeProfile());
            int originalLineCount = existing.getLines().size();
            given(settlementReader.findByIdentity(PLAYER_ID, sourceType(), SOURCE_ID))
                    .willReturn(Optional.of(existing));

            RewardSettlement result = create();

            assertThat(result).isSameAs(existing);
            assertThat(result.getLines()).hasSize(originalLineCount);
            verify(profileReader, never()).getActiveByCodeOrThrow(anyString());
            verify(settlementWriter, never()).saveAndFlush(any(RewardSettlement.class));
        }

        @Test
        @DisplayName("동시 생성 Unique 충돌 후 기존 Settlement를 다시 조회해 반환한다")
        void returnsWinnerAfterUniqueConflict() {
            RewardProfile profile = activeProfile();
            RewardSettlement winner = settlement(profile);
            given(settlementReader.findByIdentity(PLAYER_ID, sourceType(), SOURCE_ID))
                    .willReturn(Optional.empty());
            given(settlementReader.findByIdentityInNewTransaction(PLAYER_ID, sourceType(), SOURCE_ID))
                    .willReturn(Optional.of(winner));
            given(profileReader.getActiveByCodeOrThrow(PROFILE_CODE)).willReturn(profile);
            given(settlementWriter.saveAndFlush(any(RewardSettlement.class)))
                    .willThrow(new DataIntegrityViolationException("duplicate settlement identity"));

            RewardSettlement result = create();

            assertThat(result).isSameAs(winner);
            verify(settlementReader)
                    .findByIdentityInNewTransaction(PLAYER_ID, sourceType(), SOURCE_ID);
        }

        @Test
        @DisplayName("Unique 충돌 뒤 기존 Settlement가 없으면 원래 저장 예외를 전파한다")
        void rethrowsWhenConflictIsNotIdentityDuplicate() {
            RewardProfile profile = activeProfile();
            given(settlementReader.findByIdentity(PLAYER_ID, sourceType(), SOURCE_ID))
                    .willReturn(Optional.empty());
            given(settlementReader.findByIdentityInNewTransaction(PLAYER_ID, sourceType(), SOURCE_ID))
                    .willReturn(Optional.empty());
            given(profileReader.getActiveByCodeOrThrow(PROFILE_CODE)).willReturn(profile);
            given(settlementWriter.saveAndFlush(any(RewardSettlement.class)))
                    .willThrow(new DataIntegrityViolationException("other constraint"));

            assertThatThrownBy(RewardSettlementCreateServiceTest.this::create)
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessage("other constraint");
        }
    }

    private RewardSettlement create() {
        return service.create(PLAYER_ID, sourceType(), SOURCE_ID, PROFILE_CODE);
    }

    private RewardSettlement settlement(RewardProfile profile) {
        return RewardSettlement.create(PLAYER_ID, sourceType(), SOURCE_ID, profile);
    }

    private RewardSettlementSourceType sourceType() {
        return RewardSettlementSourceType.QUEST_COMPLETION;
    }

    private RewardProfile activeProfile() {
        RewardProfile profile = RewardProfile.create(
                PROFILE_CODE, "EXP 10 Profile", RewardProfileStatus.ACTIVE
        );
        ReflectionTestUtils.setField(profile, "id", 10L);
        RewardDefinition definition = RewardDefinition.create(
                "RD_EXP_10", "EXP 10", RewardType.EXP, 10L, null, true
        );
        ReflectionTestUtils.setField(definition, "id", 20L);
        profile.addLine(definition, 0, null);
        return profile;
    }

    private void assertRewardError(Runnable action, RewardError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(error)
                );
    }
}
