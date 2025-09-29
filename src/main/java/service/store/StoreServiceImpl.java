package service.store;

import java.util.List;
import model.Card;
import model.CardPack;
import model.Player;
import service.store.PurchaseResult;
import service.store.PurchaseResult.PurchaseStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StoreServiceImpl implements StoreService {
    
    private static final Logger logger = LoggerFactory.getLogger(StoreServiceImpl.class);
    private final CardPackFactory cardPackFactory;

    @Autowired
    public StoreServiceImpl(CardPackFactory cardPackFactory) {
        this.cardPackFactory = cardPackFactory;
    }

    @Override
    public PurchaseResult purchaseCardPack(Player player, String packType) {
        try {
            CardPack pack = cardPackFactory.createCardPack(packType);

            if (player.getCoins() < pack.getCost()) {
                logger.warn("{} tried to buy {} but doesn't have enough coins (has: {}, needs: {})", 
                           player.getNickname(), pack.getName(), player.getCoins(), pack.getCost());
                return PurchaseResult.failure(PurchaseStatus.INSUFFICIENT_FUNDS);
            }

            List<Card> newCards = pack.open();
            if (newCards.isEmpty()) {
                logger.warn("{} failed to get cards from pack {}. Probably out of stock.", 
                           player.getNickname(), pack.getName());
                return PurchaseResult.failure(PurchaseStatus.OUT_OF_STOCK);
            }
            
            player.setCoins(player.getCoins() - pack.getCost());
            player.getCardCollection().addAll(newCards);

            logger.info("{} bought a {} for {} coins and got {} cards.", 
                       player.getNickname(), pack.getName(), pack.getCost(), newCards.size());
            return PurchaseResult.success(newCards);

        } catch (Exception e) {
            logger.error("Error buying pack {} for player {}: {}", 
                        packType, player.getId(), e.getMessage(), e);
            return PurchaseResult.failure(PurchaseStatus.PACK_NOT_FOUND);
        }
    }
}
