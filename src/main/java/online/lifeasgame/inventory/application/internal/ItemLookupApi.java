package online.lifeasgame.inventory.application.internal;

public interface ItemLookupApi {

    ItemReference getByCode(String code);

    ItemReference getById(Long itemId);

    record ItemReference(Long id, String code) {
    }
}
