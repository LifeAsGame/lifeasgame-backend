package online.lifeasgame.inventory.infra;

import jakarta.persistence.criteria.Predicate;
import online.lifeasgame.inventory.domain.Item;
import online.lifeasgame.inventory.domain.ItemCategory;
import online.lifeasgame.inventory.domain.ItemType;
import online.lifeasgame.inventory.domain.Rarity;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

final class ItemSpecifications {

    private ItemSpecifications() {
    }

    static Specification<Item> search(String name, ItemCategory itemCategory, ItemType itemType, Rarity rarity) {
        return (root, cq, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            if (name != null && !name.isBlank()) {
                String escaped = name.trim().toLowerCase()
                        .replace("\\", "\\\\")
                        .replace("%", "\\%")
                        .replace("_", "\\_");
                ps.add(cb.like(cb.lower(root.get("name").get("value")), "%" + escaped + "%", '\\'));
            }
            if (itemCategory != null) ps.add(cb.equal(root.get("category"), itemCategory));
            if (itemType != null) ps.add(cb.equal(root.get("type"), itemType));
            if (rarity != null) ps.add(cb.equal(root.get("rarity"), rarity));

            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
