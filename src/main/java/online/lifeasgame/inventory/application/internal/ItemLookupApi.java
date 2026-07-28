package online.lifeasgame.inventory.application.internal;

public interface ItemLookupApi {

    ItemReference getByCode(String code);

    record ItemReference(Long id, String code) {
    }
}
