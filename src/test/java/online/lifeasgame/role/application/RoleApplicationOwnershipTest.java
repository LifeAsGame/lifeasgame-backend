package online.lifeasgame.role.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.role.application.command.RoleCommand;
import online.lifeasgame.role.application.query.RoleQuery;
import online.lifeasgame.role.domain.Role;
import online.lifeasgame.role.domain.RoleType;
import online.lifeasgame.role.domain.error.RoleError;
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
class RoleApplicationOwnershipTest {

    private static final Long PLAYER_ID = 234L;

    @Mock
    private RoleReader reader;

    @Mock
    private RoleWriter writer;

    @Mock
    private RoleQuery query;

    @Mock
    private CurrentPlayerAccessor currentPlayerAccessor;

    @InjectMocks
    private RoleService service;

    @InjectMocks
    private RoleQueryService queryService;

    @BeforeEach
    void currentPlayer() {
        given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(PLAYER_ID);
    }

    @Test
    void writeServiceUsesCurrentPlayerForAllOwnership() {
        Role role = role();
        given(writer.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(reader.getOwned(10L, PLAYER_ID)).willReturn(role);

        var created = service.create(new RoleCommand.Create("WORK", "Developer", null));
        service.update(10L, new RoleCommand.Update("FAMILY", "Parent", null));
        service.archive(10L);

        assertThat(created.playerId()).isEqualTo(PLAYER_ID);
        verify(reader, times(2)).getOwned(10L, PLAYER_ID);
    }

    @Test
    void queryServiceScopesListAndMissingDetailToCurrentPlayer() {
        given(query.findActive(PLAYER_ID)).willReturn(List.of());
        given(query.findOwned(10L, PLAYER_ID)).willReturn(Optional.empty());

        assertThat(queryService.list()).isEmpty();
        assertThatThrownBy(() -> queryService.detail(10L))
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(RoleError.ROLE_NOT_FOUND)
                );

        verify(query).findActive(PLAYER_ID);
        verify(query).findOwned(10L, PLAYER_ID);
    }

    private Role role() {
        return Role.create(PLAYER_ID, RoleType.of("WORK"), "Developer", null);
    }
}
