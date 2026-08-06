package online.lifeasgame.role.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.role.application.command.RoleCommand;
import online.lifeasgame.role.application.result.RoleResult;
import online.lifeasgame.role.domain.Role;
import online.lifeasgame.role.domain.RoleType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleReader reader;
    private final RoleWriter writer;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    @Transactional
    public RoleResult.Detail create(RoleCommand.Create command) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        Role saved = writer.save(Role.create(
                playerId,
                RoleType.of(command.roleType()),
                command.name(),
                command.description()
        ));
        return RoleResult.Detail.from(saved);
    }

    @Transactional
    public RoleResult.Detail update(
            Long roleId,
            RoleCommand.Update command
    ) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        Role role = reader.getOwned(roleId, playerId);
        role.update(
                RoleType.of(command.roleType()),
                command.name(),
                command.description()
        );
        return RoleResult.Detail.from(writer.save(role));
    }

    @Transactional
    public void archive(Long roleId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        Role role = reader.getOwned(roleId, playerId);
        role.archive();
        writer.save(role);
    }
}
