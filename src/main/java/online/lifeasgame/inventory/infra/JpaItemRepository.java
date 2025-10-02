package online.lifeasgame.inventory.infra;

import online.lifeasgame.inventory.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    @Query(
            """
                SELECT (COUNT(i) > 0)
                FROM Item i
                WHERE LOWER(i.name.value) = LOWER(:name)
            """
    )
    boolean existsByName(@Param("name") String name);
}
