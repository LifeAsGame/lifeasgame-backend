package online.lifeasgame.role.application;

import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.person.application.PersonFacade;
import online.lifeasgame.person.application.PersonService;
import online.lifeasgame.person.application.command.PersonCommand;
import online.lifeasgame.role.application.command.RoleCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RolePersonFacadeOwnershipTest {

    private static final Long PLAYER_ID = 234L;

    @Mock
    private RoleService roleService;

    @Mock
    private PersonService personService;

    @Mock
    private CurrentPlayerAccessor currentPlayerAccessor;

    @InjectMocks
    private RoleFacade roleFacade;

    @InjectMocks
    private PersonFacade personFacade;

    @Test
    void roleFacadeUsesOnlyCurrentPlayerOwnership() {
        RoleCommand.Create create = new RoleCommand.Create("WORK", "Developer", null);
        RoleCommand.Update update = new RoleCommand.Update("FAMILY", "Parent", null);
        given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(PLAYER_ID);

        roleFacade.create(create);
        roleFacade.list();
        roleFacade.detail(10L);
        roleFacade.update(10L, update);
        roleFacade.archive(10L);

        verify(roleService).create(PLAYER_ID, create);
        verify(roleService).list(PLAYER_ID);
        verify(roleService).detail(PLAYER_ID, 10L);
        verify(roleService).update(PLAYER_ID, 10L, update);
        verify(roleService).archive(PLAYER_ID, 10L);
    }

    @Test
    void personFacadeUsesOnlyCurrentPlayerOwnership() {
        PersonCommand.Create create = new PersonCommand.Create("Alice", null, null, null);
        PersonCommand.Update update = new PersonCommand.Update("Bob", null, null, null);
        given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(PLAYER_ID);

        personFacade.create(create);
        personFacade.list();
        personFacade.detail(20L);
        personFacade.update(20L, update);
        personFacade.archive(20L);

        verify(personService).create(PLAYER_ID, create);
        verify(personService).list(PLAYER_ID);
        verify(personService).detail(PLAYER_ID, 20L);
        verify(personService).update(PLAYER_ID, 20L, update);
        verify(personService).archive(PLAYER_ID, 20L);
    }
}
