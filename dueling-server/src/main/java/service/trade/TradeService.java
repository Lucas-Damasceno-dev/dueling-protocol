package service.trade;

import model.TradeProposal;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TradeService {

    private final Map<String, TradeProposal> activeTrades = new ConcurrentHashMap<>();

    /**
     * Creates and stores a new trade proposal.
     *
     * @param proposal The {@link TradeProposal} object to be created.
     */
    public void createTrade(TradeProposal proposal) {
        activeTrades.put(proposal.getTradeId(), proposal);
    }

    /**
     * Finds a trade proposal by its unique identifier.
     *
     * @param tradeId The unique ID of the trade proposal.
     * @return An {@link Optional} containing the {@link TradeProposal} if found, otherwise empty.
     */
    public Optional<TradeProposal> findTradeById(String tradeId) {
        return Optional.ofNullable(activeTrades.get(tradeId));
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
