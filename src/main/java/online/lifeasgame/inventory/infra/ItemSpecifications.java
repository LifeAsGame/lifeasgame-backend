package online.lifeasgame.inventory.infra;

import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

import online.lifeasgame.inventory.domain.Item;
import online.lifeasgame.inventory.domain.ItemCategory;
import online.lifeasgame.inventory.domain.ItemType;
import online.lifeasgame.inventory.domain.Rarity;
import org.springframework.data.jpa.domain.Specification;

final class ItemSpecifications {
    private ItemSpecifications() {
    }

    static Specification<Item> search(String name, ItemCategory category, ItemType type, Rarity rarity) {
        return (root, cq, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            if (name != null && !name.isBlank()) {
                ps.add(cb.like(cb.lower(root.get("name").get("value")), "%" + name.trim().toLowerCase() + "%"));
            }
            if (category != null) ps.add(cb.equal(root.get("category"), category));
            if (type != null) ps.add(cb.equal(root.get("type"), type));
            if (rarity != null) ps.add(cb.equal(root.get("rarity"), rarity));

            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
