package online.lifeasgame.economy.infra;

import online.lifeasgame.economy.domain.PriceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPriceSnapshotRepository extends JpaRepository<PriceSnapshot, Long> {
}
