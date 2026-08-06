package online.lifeasgame.role.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.role.application.command.RoleCommand;
import online.lifeasgame.role.application.result.RoleResult;
import online.lifeasgame.role.domain.Role;
import online.lifeasgame.role.domain.RoleType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleReader reader;
    private final RoleWriter writer;

    @Transactional
    public RoleResult.Detail create(Long playerId, RoleCommand.Create command) {
        Role saved = writer.save(Role.create(
                playerId,
                RoleType.of(command.roleType()),
                command.name(),
                command.description()
        ));
        return RoleResult.Detail.from(saved);
    }

    @Transactional(readOnly = true)
    public List<RoleResult.Detail> list(Long playerId) {
        return reader.findActive(playerId).stream()
                .map(RoleResult.Detail::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleResult.Detail detail(Long playerId, Long roleId) {
        return RoleResult.Detail.from(reader.getOwned(roleId, playerId));
    }

    @Transactional
    public RoleResult.Detail update(
            Long playerId,
            Long roleId,
            RoleCommand.Update command
    ) {
        Role role = reader.getOwned(roleId, playerId);
        role.update(
                RoleType.of(command.roleType()),
                command.name(),
                command.description()
        );
        return RoleResult.Detail.from(writer.save(role));
    }

    @Transactional
    public void archive(Long playerId, Long roleId) {
        Role role = reader.getOwned(roleId, playerId);
        role.archive();
        writer.save(role);
    }
}
