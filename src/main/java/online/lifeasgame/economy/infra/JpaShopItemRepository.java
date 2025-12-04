package online.lifeasgame.economy.infra;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import online.lifeasgame.economy.domain.ShopItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

public interface JpaShopItemRepository extends JpaRepository<ShopItem, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("SELECT s FROM ShopItem s WHERE s.id = :id")
    Optional<ShopItem> findByIdForUpdate(Long id);

    List<ShopItem> findByAvailableTrue();
}
