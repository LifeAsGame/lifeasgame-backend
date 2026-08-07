package online.lifeasgame.role.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.error.ErrorCode;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.person.application.internal.PersonLookupApi;
import online.lifeasgame.person.application.internal.PersonLookupApi.PersonReference;
import online.lifeasgame.person.domain.error.PersonError;
import online.lifeasgame.role.application.command.RoleRelationCommand;
import online.lifeasgame.role.application.query.RoleRelationQuery;
import online.lifeasgame.role.application.result.RoleRelationResult;
import online.lifeasgame.role.domain.Role;
import online.lifeasgame.role.domain.RoleRelation;
import online.lifeasgame.role.domain.RoleRelationStatus;
import online.lifeasgame.role.domain.RoleRelationType;
import online.lifeasgame.role.domain.RoleType;
import online.lifeasgame.role.domain.error.RoleError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RoleRelationApplicationTest {

    private static final Long PLAYER_ID = 1L;
    private static final Long ROLE_ID = 2L;
    private static final Long PERSON_ID = 3L;

    @Mock
    private RoleReader roleReader;

    @Mock
    private RoleRelationReader relationReader;

    @Mock
    private RoleRelationWriter relationWriter;

    @Mock
    private RoleRelationQuery query;

    @Mock
    private PersonLookupApi personLookupApi;

    @Mock
    private CurrentPlayerAccessor currentPlayerAccessor;

    @InjectMocks
    private RoleRelationService service;

    @InjectMocks
    private RoleRelationQueryService queryService;

    @BeforeEach
    void currentPlayer() {
        given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(PLAYER_ID);
    }

    @Test
    void createsSameOwnerRelationUsingCurrentPlayer() {
        willReturn(role()).given(roleReader).getOwnedForUpdate(ROLE_ID, PLAYER_ID);
        given(relationReader.findPair(ROLE_ID, PERSON_ID, PLAYER_ID))
                .willReturn(Optional.empty());
        given(personLookupApi.getOwnedActive(PERSON_ID, PLAYER_ID)).willReturn(person());
        given(relationWriter.saveAndFlush(any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(ROLE_ID, create());

        assertThat(result.playerId()).isEqualTo(PLAYER_ID);
        assertThat(result.roleId()).isEqualTo(ROLE_ID);
        assertThat(result.personId()).isEqualTo(PERSON_ID);
        assertThat(result.personDisplayName()).isEqualTo("Alice");
    }

    @Test
    void rejectsActiveDuplicateBeforePersonLookup() {
        RoleRelation existing = relation();
        given(roleReader.getOwnedForUpdate(ROLE_ID, PLAYER_ID)).willReturn(role());
        given(relationReader.findPair(ROLE_ID, PERSON_ID, PLAYER_ID))
                .willReturn(Optional.of(existing));

        assertError(
                () -> service.create(ROLE_ID, create()),
                RoleError.ROLE_RELATION_ALREADY_EXISTS
        );
        verifyNoInteractions(personLookupApi);
    }

    @Test
    void reactivatesArchivedPairOnTheSameAggregate() {
        RoleRelation existing = relation();
        existing.archive();
        given(roleReader.getOwnedForUpdate(ROLE_ID, PLAYER_ID)).willReturn(role());
        given(relationReader.findPair(ROLE_ID, PERSON_ID, PLAYER_ID))
                .willReturn(Optional.of(existing));
        given(personLookupApi.getOwnedActive(PERSON_ID, PLAYER_ID)).willReturn(person());
        given(relationWriter.saveAndFlush(existing)).willReturn(existing);

        var result = service.create(
                ROLE_ID,
                new RoleRelationCommand.Create(PERSON_ID, "FRIEND", "new")
        );

        assertThat(existing.getStatus()).isEqualTo(RoleRelationStatus.ACTIVE);
        assertThat(existing.getRelationType().value()).isEqualTo("FRIEND");
        assertThat(result.status()).isEqualTo("ACTIVE");
        verify(relationWriter).saveAndFlush(existing);
    }

    @Test
    void preservesCrossOwnerRolePersonAndRelationErrors() {
        given(roleReader.getOwnedForUpdate(ROLE_ID, PLAYER_ID)).willThrow(
                new DomainException(RoleError.ROLE_NOT_FOUND)
        );
        assertError(
                () -> service.create(ROLE_ID, create()),
                RoleError.ROLE_NOT_FOUND
        );

        willReturn(role()).given(roleReader).getOwnedForUpdate(ROLE_ID, PLAYER_ID);
        given(relationReader.findPair(ROLE_ID, PERSON_ID, PLAYER_ID))
                .willReturn(Optional.empty());
        given(personLookupApi.getOwnedActive(PERSON_ID, PLAYER_ID)).willThrow(
                new DomainException(PersonError.PERSON_NOT_FOUND)
        );
        assertError(
                () -> service.create(ROLE_ID, create()),
                PersonError.PERSON_NOT_FOUND
        );

        given(roleReader.getOwned(ROLE_ID, PLAYER_ID)).willReturn(role());
        given(relationReader.getOwned(9L, ROLE_ID, PLAYER_ID)).willThrow(
                new DomainException(RoleError.ROLE_RELATION_NOT_FOUND)
        );
        assertError(
                () -> service.update(
                        ROLE_ID,
                        9L,
                        new RoleRelationCommand.Update("FRIEND", null)
                ),
                RoleError.ROLE_RELATION_NOT_FOUND
        );
    }

    @Test
    void rejectsCreateForArchivedRole() {
        Role role = role();
        role.archive();
        given(roleReader.getOwnedForUpdate(ROLE_ID, PLAYER_ID)).willReturn(role);

        assertError(
                () -> service.create(ROLE_ID, create()),
                RoleError.ROLE_ARCHIVED
        );
    }

    @Test
    void queryServiceUsesBatchPersonLookup() {
        RoleRelationResult.Stored stored = new RoleRelationResult.Stored(
                9L,
                PLAYER_ID,
                ROLE_ID,
                PERSON_ID,
                "FAMILY",
                null,
                "ACTIVE",
                null,
                null,
                0L
        );
        given(roleReader.getOwned(ROLE_ID, PLAYER_ID)).willReturn(role());
        given(query.findActive(PLAYER_ID, ROLE_ID)).willReturn(List.of(stored));
        given(personLookupApi.findOwnedByIds(
                java.util.Set.of(PERSON_ID),
                PLAYER_ID
        )).willReturn(Map.of(PERSON_ID, person()));

        var result = queryService.list(ROLE_ID);

        assertThat(result).singleElement()
                .extracting(RoleRelationResult.Detail::personDisplayName)
                .isEqualTo("Alice");
    }

    private Role role() {
        return Role.create(PLAYER_ID, RoleType.of("SELF"), "Self", null);
    }

    private RoleRelation relation() {
        return RoleRelation.create(
                PLAYER_ID,
                ROLE_ID,
                PERSON_ID,
                RoleRelationType.of("FAMILY"),
                null
        );
    }

    private PersonReference person() {
        return new PersonReference(PERSON_ID, null, "Alice");
    }

    private RoleRelationCommand.Create create() {
        return new RoleRelationCommand.Create(PERSON_ID, "FAMILY", null);
    }

    private void assertError(Runnable action, ErrorCode error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(error)
                );
    }
}
