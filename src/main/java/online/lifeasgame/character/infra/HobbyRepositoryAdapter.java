package online.lifeasgame.character.infra;


import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.Hobby;
import online.lifeasgame.character.domain.HobbyCategory;
import online.lifeasgame.character.domain.repository.HobbyRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class HobbyRepositoryAdapter implements HobbyRepository {

    private final JpaHobbyRepository jpaRepository;

    @Override
    public Hobby save(Hobby Hobby) {
        return jpaRepository.save(Hobby);
    }

    @Override
    public List<Hobby> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<Hobby> findByCategoryIn(List<HobbyCategory> HobbyCategories) {
        return jpaRepository.findByCategoryIn(HobbyCategories);
    }

    @Override
    public Optional<Hobby> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public void delete(Long hobbyId) {
        jpaRepository.deleteById(hobbyId);
    }
}
