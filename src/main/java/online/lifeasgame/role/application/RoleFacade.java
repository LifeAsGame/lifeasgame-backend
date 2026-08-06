package online.lifeasgame.role.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.role.application.command.RoleCommand;
import online.lifeasgame.role.application.result.RoleResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RoleFacade {

    private final RoleService service;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    public RoleResult.Detail create(RoleCommand.Create command) {
        return service.create(currentPlayerAccessor.currentPlayerIdOrThrow(), command);
    }

    public List<RoleResult.Detail> list() {
        return service.list(currentPlayerAccessor.currentPlayerIdOrThrow());
    }

    public RoleResult.Detail detail(Long roleId) {
        return service.detail(currentPlayerAccessor.currentPlayerIdOrThrow(), roleId);
    }

    public RoleResult.Detail update(Long roleId, RoleCommand.Update command) {
        return service.update(
                currentPlayerAccessor.currentPlayerIdOrThrow(),
                roleId,
                command
        );
    }

    public void archive(Long roleId) {
        service.archive(currentPlayerAccessor.currentPlayerIdOrThrow(), roleId);
    }
}
