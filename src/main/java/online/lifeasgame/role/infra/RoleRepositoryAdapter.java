package online.lifeasgame.role.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.role.domain.Role;
import online.lifeasgame.role.domain.repository.RoleRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepository {

    private final JpaRoleRepository repository;

    @Override
    public Role save(Role role) {
        return repository.save(role);
    }

    @Override
    public Optional<Role> findByIdAndPlayerId(Long id, Long playerId) {
        return repository.findByIdAndPlayerId(id, playerId);
    }

    @Override
    public Optional<Role> findByIdAndPlayerIdForUpdate(Long id, Long playerId) {
        return repository.findByIdAndPlayerIdForUpdate(id, playerId);
    }
}
