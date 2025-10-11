package service.store;

import java.util.List;
import model.Card;
import model.CardPack;
import model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import service.election.LeaderElectionService;
import api.ServerApiClient;

@Profile("server")
import org.springframework.context.annotation.Profile;

@Profile("server")
@Service
public class StoreServiceImpl implements StoreService {
    
    private static final Logger logger = LoggerFactory.getLogger(StoreServiceImpl.class);
    private final CardPackFactory cardPackFactory;
    private final LeaderElectionService leaderElectionService;
    private final ServerApiClient serverApiClient;

    @Autowired
    public StoreServiceImpl(CardPackFactory cardPackFactory, 
                            LeaderElectionService leaderElectionService, 
                            ServerApiClient serverApiClient) {
        this.cardPackFactory = cardPackFactory;
        this.leaderElectionService = leaderElectionService;
        this.serverApiClient = serverApiClient;
    }

    @Override
    public PurchaseResult purchaseCardPack(Player player, String packType) {
        CardPack pack = cardPackFactory.createCardPack(packType);

        if (player.getCoins() < pack.getCost()) {
            logger.warn("{} tried to buy {} but doesn't have enough coins (has: {}, needs: {})", 
                       player.getNickname(), pack.getName(), player.getCoins(), pack.getCost());
            return PurchaseResult.failure(PurchaseResult.PurchaseStatus.INSUFFICIENT_FUNDS);
        }

        String leader = leaderElectionService.getLeader();
        if (leader == null) {
            logger.error("No leader found for distributed lock.");
            return PurchaseResult.failure(PurchaseResult.PurchaseStatus.PACK_NOT_FOUND);
        }

        boolean lockAcquired = false;
        try {
            lockAcquired = serverApiClient.acquireLock(leader);
            if (!lockAcquired) {
                logger.warn("Could not acquire distributed lock for purchase by {}.", player.getNickname());
                return PurchaseResult.failure(PurchaseResult.PurchaseStatus.PACK_NOT_FOUND);
            }

            List<Card> newCards = pack.open();
            if (newCards.isEmpty()) {
                logger.warn("{} failed to get cards from pack {}. Probably out of stock.", 
                           player.getNickname(), pack.getName());
                return PurchaseResult.failure(PurchaseResult.PurchaseStatus.OUT_OF_STOCK);
            }
            
            player.setCoins(player.getCoins() - pack.getCost());
            player.getCardCollection().addAll(newCards);

            logger.info("{} bought a {} for {} coins and got {} cards.", 
                       player.getNickname(), pack.getName(), pack.getCost(), newCards.size());
            return PurchaseResult.success(newCards);

        } catch (Exception e) {
            logger.error("Error buying pack {} for player {}: {}", 
                        packType, player.getId(), e.getMessage(), e);
            return PurchaseResult.failure(PurchaseResult.PurchaseStatus.PACK_NOT_FOUND);
        } finally {
            if (lockAcquired) {
                serverApiClient.releaseLock(leader);
            }
        }
    }
}