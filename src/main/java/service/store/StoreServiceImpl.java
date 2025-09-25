package service.store;

import java.util.List;
import model.Card;
import model.CardPack;
import model.Player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StoreServiceImpl implements StoreService {
    
    private static final Logger logger = LoggerFactory.getLogger(StoreServiceImpl.class);

    @Override
    public boolean purchaseCardPack(Player player, String packType) {
        try {
            CardPack pack = CardPackFactory.createCardPack(packType);
            
            synchronized (player) {
                if (player.getCoins() >= pack.getCost()) {
                    player.setCoins(player.getCoins() - pack.getCost());
                    
                    List<Card> newCards = pack.open();
                    
                    player.getCardCollection().addAll(newCards);
                    
                    logger.info("{} comprou um {} por {} moedas", 
                               player.getNickname(), pack.getName(), pack.getCost());
                    return true;
                } else {
                    logger.warn("{} tentou comprar {} mas não tem moedas suficientes (tem: {}, precisa: {})", 
                               player.getNickname(), pack.getName(), player.getCoins(), pack.getCost());
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao comprar pacote {} para o jogador {}: {}", 
                        packType, player.getId(), e.getMessage(), e);
            return false;
        }
    }
}