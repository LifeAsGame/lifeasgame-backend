package online.lifeasgame.role.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.role.application.query.RoleQuery;
import online.lifeasgame.role.application.result.RoleResult;
import online.lifeasgame.role.domain.RoleStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoleQueryAdapter implements RoleQuery {

    private final JpaRoleRepository repository;

    @Override
    public List<RoleResult.Detail> findActive(Long playerId) {
        return repository.findAllByPlayerIdAndStatusOrderByIdAsc(
                        playerId,
                        RoleStatus.ACTIVE
                ).stream()
                .map(RoleResult.Detail::from)
                .toList();
    }

    @Override
    public Optional<RoleResult.Detail> findOwned(Long roleId, Long playerId) {
        return repository.findByIdAndPlayerId(roleId, playerId)
                .map(RoleResult.Detail::from);
    }
}
