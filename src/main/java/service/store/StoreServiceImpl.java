package service.store;

import java.util.List;
import model.Card;
import model.CardPack;
import model.Player;
import service.store.PurchaseResult;
import service.store.PurchaseResult.PurchaseStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of the StoreService interface.
 * Handles the purchase of card packs, including stock management and player currency deduction.
 */
public class StoreServiceImpl implements StoreService {
    
    private static final Logger logger = LoggerFactory.getLogger(StoreServiceImpl.class);
    private static final ReentrantLock packPurchaseLock = new ReentrantLock(true); // Fair lock

    /**
     * {@inheritDoc}
     * Purchases a card pack for a player, deducting the cost from their coins and adding
     * the cards to their collection. This method is thread-safe to prevent race conditions
     * when multiple players try to purchase packs simultaneously.
     *
     * @param player the player purchasing the card pack
     * @param packType the type of card pack to purchase (e.g., "BASIC", "PREMIUM", "LEGENDARY")
     * @return a PurchaseResult indicating the success or failure of the purchase
     */
    @Override
    public PurchaseResult purchaseCardPack(Player player, String packType) {
        try {
            CardPack pack = CardPackFactory.createCardPack(packType);

            if (player.getCoins() < pack.getCost()) {
                logger.warn("{} tried to buy {} but doesn't have enough coins (has: {}, needs: {})", 
                           player.getNickname(), pack.getName(), player.getCoins(), pack.getCost());
                return PurchaseResult.failure(PurchaseStatus.INSUFFICIENT_FUNDS);
            }

            List<Card> newCards;
            packPurchaseLock.lock();
            try {
                newCards = pack.open();
                if (newCards.isEmpty()) {
                    logger.warn("{} failed to get cards from pack {}. Probably out of stock.", 
                               player.getNickname(), pack.getName());
                    return PurchaseResult.failure(PurchaseStatus.OUT_OF_STOCK);
                }
                
                // Deduct coins only if the purchase is successful
                player.setCoins(player.getCoins() - pack.getCost());
                player.getCardCollection().addAll(newCards);
            } finally {
                packPurchaseLock.unlock();
            }

            logger.info("{} bought a {} for {} coins and got {} cards.", 
                       player.getNickname(), pack.getName(), pack.getCost(), newCards.size());
            return PurchaseResult.success(newCards);

        } catch (Exception e) {
            logger.error("Error buying pack {} for player {}: {}", 
                        packType, player.getId(), e.getMessage(), e);
            // In case of error, ideally the transaction would be rolled back, but for simplicity, we just log.
            return PurchaseResult.failure(PurchaseStatus.PACK_NOT_FOUND); // Generic for other errors
        }
    }
}
