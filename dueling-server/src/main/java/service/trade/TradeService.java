package service.trade;

import model.TradeProposal;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Service
public class TradeService {

    private static final Logger logger = LoggerFactory.getLogger(TradeService.class);
    private final RMap<String, TradeProposal> activeTrades;

    @Autowired
    public TradeService(RedissonClient redissonClient) {
        // Use JsonJacksonCodec.INSTANCE for consistent serialization across servers
        this.activeTrades = redissonClient.getMap("dueling:activeTrades", JsonJacksonCodec.INSTANCE);
        logger.info("TradeService initialized with Redis-backed storage (map: dueling:activeTrades)");
    }

    /**
     * Creates and stores a new trade proposal in Redis (distributed).
     *
     * @param proposal The {@link TradeProposal} object to be created.
     */
    public void createTrade(TradeProposal proposal) {
        logger.info("[TRADE-REDIS] Storing trade proposal {} in Redis", proposal.getTradeId());
        activeTrades.put(proposal.getTradeId(), proposal);
        logger.info("[TRADE-REDIS] Trade proposal {} stored. Total trades in Redis: {}", proposal.getTradeId(), activeTrades.size());
    }

    /**
     * Updates an existing trade proposal in Redis.
     *
     * @param proposal The {@link TradeProposal} object to be updated.
     */
    public void updateTrade(TradeProposal proposal) {
        logger.info("[TRADE-REDIS] Updating trade proposal {} in Redis with status {}", proposal.getTradeId(), proposal.getStatus());
        activeTrades.put(proposal.getTradeId(), proposal);
        logger.info("[TRADE-REDIS] Trade proposal {} updated successfully", proposal.getTradeId());
    }

    /**
     * Finds a trade proposal by its unique identifier.
     *
     * @param tradeId The unique ID of the trade proposal.
     * @return An {@link Optional} containing the {@link TradeProposal} if found, otherwise empty.
     */
    public Optional<TradeProposal> findTradeById(String tradeId) {
        logger.info("[TRADE-REDIS] Looking for trade proposal {} in Redis", tradeId);
        logger.info("[TRADE-REDIS] Total trades in Redis: {}", activeTrades.size());
        TradeProposal proposal = activeTrades.get(tradeId);
        if (proposal != null) {
            logger.info("[TRADE-REDIS] Trade proposal {} FOUND in Redis with status {}", tradeId, proposal.getStatus());
        } else {
            logger.warn("[TRADE-REDIS] Trade proposal {} NOT FOUND in Redis", tradeId);
        }
        return Optional.ofNullable(proposal);
    }

    /**
     * Removes a trade proposal from the active trades.
     *
     * @param tradeId The unique ID of the trade proposal to remove.
     */
    public void removeTrade(String tradeId) {
        activeTrades.remove(tradeId);
    }
}
