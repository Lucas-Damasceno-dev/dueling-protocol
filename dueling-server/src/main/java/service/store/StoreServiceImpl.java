package service.store;

import java.util.ArrayList;
import java.util.List;
import model.Card;
import model.CardPack;
import model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import service.lock.LockService;
import service.blockchain.BlockchainService;

@Service
@Transactional
public class StoreServiceImpl implements StoreService {
    
    private static final Logger logger = LoggerFactory.getLogger(StoreServiceImpl.class);
    private final CardPackFactory cardPackFactory;
    private final LockService lockService;
    private final BlockchainService blockchainService;

    @Autowired
    public StoreServiceImpl(CardPackFactory cardPackFactory, LockService lockService, BlockchainService blockchainService) {
        this.cardPackFactory = cardPackFactory;
        this.lockService = lockService;
        this.blockchainService = blockchainService;
    }

    @Override
    public PurchaseResult purchaseCardPack(Player player, String packType) {
        logger.debug("Attempting to purchase pack of type '{}' for player '{}'", packType, player != null ? player.getId() : "null");
        CardPack pack = cardPackFactory.createCardPack(packType);

        if (player.getCoins() < pack.getCost()) {
            logger.warn("{} tried to buy {} but doesn't have enough coins (has: {}, needs: {})", 
                       player.getNickname(), pack.getName(), player.getCoins(), pack.getCost());
            return PurchaseResult.failure(PurchaseResult.PurchaseStatus.INSUFFICIENT_FUNDS);
        }

        boolean lockAcquired = false;
        try {
            lockAcquired = lockService.acquire();
            if (!lockAcquired) {
                logger.warn("Could not acquire distributed lock for purchase by {}.", player.getNickname());
                // Retornar um status mais apropriado, como SERVER_BUSY ou CONFLICT
                return PurchaseResult.failure(PurchaseResult.PurchaseStatus.SERVER_BUSY);
            }

            // A lógica de abrir o pacote agora usa o CardRepository com estoque no Redis
            List<Card> newCards = pack.open();
            if (newCards.isEmpty()) {
                logger.warn("{} failed to get cards from pack {}. Probably out of stock.", 
                           player.getNickname(), pack.getName());
                // Tentar resetar o estoque e tentar novamente
                cardPackFactory.getCardRepository().resetStockIfDepleted();
                // Tentar abrir o pacote novamente após o reset
                newCards = pack.open();
                if (newCards.isEmpty()) {
                    logger.error("{} failed again to get cards from pack {} after stock reset.", 
                               player.getNickname(), pack.getName());
                    return PurchaseResult.failure(PurchaseResult.PurchaseStatus.OUT_OF_STOCK);
                } else {
                    logger.info("Stock reset successful, {} got {} cards after reset", 
                               player.getNickname(), newCards.size());
                }
            }
            
            // Atualiza o jogador (isso também pode precisar de sincronização dependendo de como Player é gerenciado)
            player.setCoins(player.getCoins() - pack.getCost());
            // Create new list to force JPA to detect change
            int cardsBefore = player.getCardCollection().size();
            List<Card> updatedCards = new ArrayList<>(player.getCardCollection());
            updatedCards.addAll(newCards);
            player.setCardCollection(updatedCards);
            int cardsAfter = player.getCardCollection().size();
            
            logger.info("{} bought a {} for {} coins and got {} cards. Cards before: {}, after: {}", 
                       player.getNickname(), pack.getName(), pack.getCost(), newCards.size(), cardsBefore, cardsAfter);
            
            // Record purchase on blockchain asynchronously
            blockchainService.recordPurchase(player, newCards, packType);
            
            return PurchaseResult.success(newCards);

        } catch (Exception e) {
            logger.error("Error buying pack {} for player {}: {}", 
                        packType, player.getId(), e.getMessage(), e);
            return PurchaseResult.failure(PurchaseResult.PurchaseStatus.INTERNAL_SERVER_ERROR);
        } finally {
            if (lockAcquired) {
                lockService.release();
            }
        }
    }
}