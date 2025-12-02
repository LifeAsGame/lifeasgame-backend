package online.lifeasgame.economy.domain.repository;

import online.lifeasgame.economy.domain.PriceSnapshot;

public interface PriceSnapshotRepository {
    PriceSnapshot save(PriceSnapshot snapshot);
}
