package online.lifeasgame.person.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.person.application.command.PersonCommand;
import online.lifeasgame.person.application.query.PersonQuery;
import online.lifeasgame.person.domain.Person;
import online.lifeasgame.person.domain.error.PersonError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PersonApplicationOwnershipTest {

    private static final Long PLAYER_ID = 234L;

    @Mock
    private PersonReader reader;

    @Mock
    private PersonWriter writer;

    @Mock
    private PersonQuery query;

    @Mock
    private CurrentPlayerAccessor currentPlayerAccessor;

    @InjectMocks
    private PersonService service;

    @InjectMocks
    private PersonQueryService queryService;

    @BeforeEach
    void currentPlayer() {
        given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(PLAYER_ID);
    }

    @Test
    void writeServiceUsesCurrentPlayerForAllOwnership() {
        Person person = person();
        given(writer.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(reader.getOwned(10L, PLAYER_ID)).willReturn(person);

        var created = service.create(new PersonCommand.Create("Alice", null, null, null));
        service.update(10L, new PersonCommand.Update("Bob", null, null, null));
        service.archive(10L);

        assertThat(created.ownerPlayerId()).isEqualTo(PLAYER_ID);
        assertThat(created.linkedUserId()).isNull();
        verify(reader, times(2)).getOwned(10L, PLAYER_ID);
    }

    @Test
    void queryServiceScopesListAndMissingDetailToCurrentPlayer() {
        given(query.findActive(PLAYER_ID)).willReturn(List.of());
        given(query.findOwned(10L, PLAYER_ID)).willReturn(Optional.empty());

        assertThat(queryService.list()).isEmpty();
        assertThatThrownBy(() -> queryService.detail(10L))
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(PersonError.PERSON_NOT_FOUND)
                );

        verify(query).findActive(PLAYER_ID);
        verify(query).findOwned(10L, PLAYER_ID);
    }

    private Person person() {
        return Person.create(PLAYER_ID, "Alice", null, null, null);
    }
}
