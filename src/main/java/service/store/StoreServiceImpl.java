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

public class StoreServiceImpl implements StoreService {
    
    private static final Logger logger = LoggerFactory.getLogger(StoreServiceImpl.class);
    private static final ReentrantLock packPurchaseLock = new ReentrantLock(true); // Fair lock

    @Override
    public PurchaseResult purchaseCardPack(Player player, String packType) {
        try {
            CardPack pack = CardPackFactory.createCardPack(packType);

            if (player.getCoins() < pack.getCost()) {
                logger.warn("{} tentou comprar {} mas não tem moedas suficientes (tem: {}, precisa: {})", 
                           player.getNickname(), pack.getName(), player.getCoins(), pack.getCost());
                return PurchaseResult.failure(PurchaseStatus.INSUFFICIENT_FUNDS);
            }

            List<Card> newCards;
            packPurchaseLock.lock();
            try {
                newCards = pack.open();
                if (newCards.isEmpty()) {
                    logger.warn("{} não conseguiu obter cartas do pacote {}. Provavelmente fora de estoque.", 
                               player.getNickname(), pack.getName());
                    return PurchaseResult.failure(PurchaseStatus.OUT_OF_STOCK);
                }
                
                // Debita as moedas somente se a compra for bem-sucedida
                player.setCoins(player.getCoins() - pack.getCost());
                player.getCardCollection().addAll(newCards);
            } finally {
                packPurchaseLock.unlock();
            }

            logger.info("{} comprou um {} por {} moedas e obteve {} cartas.", 
                       player.getNickname(), pack.getName(), pack.getCost(), newCards.size());
            return PurchaseResult.success(newCards);

        } catch (Exception e) {
            logger.error("Erro ao comprar pacote {} para o jogador {}: {}", 
                        packType, player.getId(), e.getMessage(), e);
            // Em caso de erro, idealmente a transação seria revertida, mas por simplicidade, apenas logamos.
            return PurchaseResult.failure(PurchaseStatus.PACK_NOT_FOUND); // Genérico para outros erros
        }
    }
}
