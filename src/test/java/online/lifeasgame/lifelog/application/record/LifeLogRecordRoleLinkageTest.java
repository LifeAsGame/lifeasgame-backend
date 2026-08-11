package online.lifeasgame.lifelog.application.record;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.lifelog.domain.error.LifeLogError;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogRecord;
import online.lifeasgame.lifelog.domain.record.LifeLogSourceType;
import online.lifeasgame.lifelog.domain.record.repository.LifeLogRecordRepository;
import online.lifeasgame.role.application.internal.RoleEventLookupApi;
import online.lifeasgame.role.application.internal.RoleLookupApi;
import online.lifeasgame.role.domain.error.RoleError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("LifeLog canonical RoleEvent linkage")
class LifeLogRecordRoleLinkageTest {

    private static final Long PLAYER_ID = 252L;
    private static final Long ROLE_ID = 25L;
    private static final Long EVENT_ID = 52L;
    private static final Instant NOW =
            Instant.parse("2026-08-11T04:00:00Z");

    @Mock
    private LifeLogRecordRepository repository;

    @Mock
    private PlayerTimezoneResolver timezoneResolver;

    @Mock
    private RoleLookupApi roleLookupApi;

    @Mock
    private RoleEventLookupApi roleEventLookupApi;

    @Mock
    private Clock clock;

    @InjectMocks
    private LifeLogRecordRegistrar registrar;

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(NOW);
        lenient().when(repository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("LifeLog에 Role context를 지정하지 않으면")
    class WithoutRoleContext {

        @Test
        @DisplayName("기존 canonical header를 동일하게 저장한다")
        void preservesLegacyHeader() {
            LifeLogRecord record = register(
                    LifeLogRecordMetadataCommand.none()
            );

            assertThat(record.getPrimaryRoleId()).isNull();
            assertThat(record.getRoleEventId()).isNull();
            assertThat(record.getOccurredAt()).isEqualTo(NOW);
            verifyNoInteractions(roleLookupApi, roleEventLookupApi);
        }
    }

    @Nested
    @DisplayName("LifeLog에 Role만 지정하면")
    class WithRoleOnly {

        @Test
        @DisplayName("subtype 없이도 owned Role을 canonical header에 저장한다")
        void storesOwnedRoleWithoutSubtype() {
            given(roleLookupApi.getOwned(ROLE_ID, PLAYER_ID)).willReturn(
                    roleReference()
            );

            LifeLogRecord record = register(new LifeLogRecordMetadataCommand(
                    null,
                    null,
                    ROLE_ID,
                    null
            ));

            assertThat(record.getPrimaryRoleId()).isEqualTo(ROLE_ID);
            assertThat(record.getRoleEventId()).isNull();
            assertThat(record.isContentReady()).isFalse();
            verifyNoInteractions(roleEventLookupApi);
        }

        @Test
        @DisplayName("다른 Player의 Role은 provider ownership 오류로 거부한다")
        void rejectsAnotherPlayersRole() {
            given(roleLookupApi.getOwned(ROLE_ID, PLAYER_ID)).willThrow(
                    new DomainException(RoleError.ROLE_NOT_FOUND)
            );

            assertThatThrownBy(() -> register(
                    new LifeLogRecordMetadataCommand(
                            null,
                            null,
                            ROLE_ID,
                            null
                    )
            )).isInstanceOfSatisfying(
                    DomainException.class,
                    exception -> assertThat(exception.getErrorCode())
                            .isEqualTo(RoleError.ROLE_NOT_FOUND)
            );
            verifyNoInteractions(repository, roleEventLookupApi);
        }
    }

    @Nested
    @DisplayName("LifeLog에 RoleEvent를 연결하면")
    class WithRoleEvent {

        @Test
        @DisplayName("eventId만 지정해도 Event의 roleId를 derive해 둘 다 저장한다")
        void derivesRoleFromEvent() {
            given(roleEventLookupApi.getOwned(EVENT_ID, PLAYER_ID))
                    .willReturn(eventReference(ROLE_ID));

            LifeLogRecord record = register(new LifeLogRecordMetadataCommand(
                    null,
                    null,
                    null,
                    EVENT_ID
            ));

            assertThat(record.getPrimaryRoleId()).isEqualTo(ROLE_ID);
            assertThat(record.getRoleEventId()).isEqualTo(EVENT_ID);
            verifyNoInteractions(roleLookupApi);
        }

        @Test
        @DisplayName("명시한 Role과 Event의 Role이 같으면 linkage를 저장한다")
        void storesMatchingRoleAndEvent() {
            given(roleEventLookupApi.getOwned(EVENT_ID, PLAYER_ID))
                    .willReturn(eventReference(ROLE_ID));
            given(roleLookupApi.getOwned(ROLE_ID, PLAYER_ID)).willReturn(
                    roleReference()
            );

            LifeLogRecord record = register(new LifeLogRecordMetadataCommand(
                    "MEMORY",
                    null,
                    ROLE_ID,
                    EVENT_ID
            ));

            assertThat(record.getPrimaryRoleId()).isEqualTo(ROLE_ID);
            assertThat(record.getRoleEventId()).isEqualTo(EVENT_ID);
            assertThat(record.isContentReady()).isTrue();
            verify(repository).saveAndFlush(record);
        }

        @Test
        @DisplayName("명시한 Role과 Event의 Role이 다르면 저장 전에 거부한다")
        void rejectsMismatchedRoleAndEvent() {
            given(roleEventLookupApi.getOwned(EVENT_ID, PLAYER_ID))
                    .willReturn(eventReference(99L));

            assertThatThrownBy(() -> register(
                    new LifeLogRecordMetadataCommand(
                            null,
                            null,
                            ROLE_ID,
                            EVENT_ID
                    )
            )).isInstanceOfSatisfying(
                    DomainException.class,
                    exception -> assertThat(exception.getErrorCode())
                            .isEqualTo(
                                    LifeLogError.ROLE_EVENT_CONTEXT_MISMATCH
                            )
            );
            verifyNoInteractions(repository, roleLookupApi);
        }

        @Test
        @DisplayName("다른 Player의 Event는 provider ownership 오류로 거부한다")
        void rejectsAnotherPlayersEvent() {
            given(roleEventLookupApi.getOwned(EVENT_ID, PLAYER_ID))
                    .willThrow(new DomainException(
                            RoleError.ROLE_EVENT_NOT_FOUND
                    ));

            assertThatThrownBy(() -> register(
                    new LifeLogRecordMetadataCommand(
                            null,
                            null,
                            null,
                            EVENT_ID
                    )
            )).isInstanceOfSatisfying(
                    DomainException.class,
                    exception -> assertThat(exception.getErrorCode())
                            .isEqualTo(RoleError.ROLE_EVENT_NOT_FOUND)
            );
            verifyNoInteractions(repository, roleLookupApi);
        }
    }

    private LifeLogRecord register(LifeLogRecordMetadataCommand metadata) {
        return registrar.register(
                PLAYER_ID,
                LifeLogSourceType.COLLECTION,
                1L,
                LifeLogEntryMode.FULL,
                metadata
        );
    }

    private RoleLookupApi.RoleReference roleReference() {
        return new RoleLookupApi.RoleReference(
                ROLE_ID,
                "Developer",
                "ACTIVE"
        );
    }

    private RoleEventLookupApi.RoleEventReference eventReference(
            Long roleId
    ) {
        return new RoleEventLookupApi.RoleEventReference(
                EVENT_ID,
                roleId,
                "PLANNED"
        );
    }
}
