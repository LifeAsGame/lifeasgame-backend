package online.lifeasgame.economy.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.PriceSnapshot;
import online.lifeasgame.economy.domain.repository.PriceSnapshotRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PriceSnapshotRepositoryAdapter implements PriceSnapshotRepository {

    private final JpaPriceSnapshotRepository jpaPriceSnapshotRepository;

    @Override
    public PriceSnapshot save(PriceSnapshot snapshot) {
        return jpaPriceSnapshotRepository.save(snapshot);
    }
}
