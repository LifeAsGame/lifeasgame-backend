package online.lifeasgame.character.infra;

import java.util.Collection;
import java.util.List;
import online.lifeasgame.character.domain.Hobby;
import online.lifeasgame.character.domain.HobbyCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaHobbyRepository extends JpaRepository<Hobby, Long> {
    List<Hobby> findByCategoryInAndActiveTrue(Collection<HobbyCategory> categories);

    List<Hobby> findByActiveTrue();

    @Query("select h from Hobby h where h.active = true and (:query is null or lower(h.name) like lower(concat('%', :query, '%'))) and (:category is null or h.category = :category)")
    Page<Hobby> search(@Param("query") String query, @Param("category") HobbyCategory category, Pageable page);
}
