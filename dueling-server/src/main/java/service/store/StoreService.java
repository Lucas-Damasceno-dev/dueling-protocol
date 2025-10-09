package service.store;

import model.Player;


/**
 * Interface for store services.
 * Defines the contract for purchasing card packs in the game store.
 */
public interface StoreService {
    /**
     * Purchases a card pack for a player.
     *
     * @param player the player purchasing the card pack
     * @param packType the type of card pack to purchase (e.g., "BASIC", "PREMIUM", "LEGENDARY")
     * @return a PurchaseResult indicating the success or failure of the purchase
     */
    PurchaseResult purchaseCardPack(Player player, String packType);
}
