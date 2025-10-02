package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.inventory.application.command.InventoryCommand;
import online.lifeasgame.inventory.application.result.InventoryResult;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryFacade {

    private final InventoryService inventoryService;
    private final CurrentPlayerAccessor currentPlayer;

    public InventoryResult.Slots add(InventoryCommand.Add cmd) {
        Long playerId = currentPlayer.currentPlayerIdOrThrow();
        return inventoryService.add(playerId, cmd);
    }

    public InventoryResult.Entries list() {
        Long playerId = currentPlayer.currentPlayerIdOrThrow();
        return inventoryService.list(playerId);
    }

    public void remove(InventoryCommand.Remove cmd) {
        Long playerId = currentPlayer.currentPlayerIdOrThrow();
        inventoryService.remove(playerId, cmd);
    }

    public void move(InventoryCommand.Move cmd) {
        Long playerId = currentPlayer.currentPlayerIdOrThrow();
        inventoryService.move(playerId, cmd);
    }

    public void merge(InventoryCommand.Merge cmd) {
        Long playerId = currentPlayer.currentPlayerIdOrThrow();
        inventoryService.merge(playerId, cmd);
    }

    public InventoryResult.Slot split(InventoryCommand.Split cmd) {
        Long playerId = currentPlayer.currentPlayerIdOrThrow();
        return inventoryService.split(playerId, cmd);
    }
}