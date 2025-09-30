package service.trade;

import model.TradeProposal;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TradeService {

    private final Map<String, TradeProposal> activeTrades = new ConcurrentHashMap<>();

    public void createTrade(TradeProposal proposal) {
        activeTrades.put(proposal.getTradeId(), proposal);
    }

    public Optional<TradeProposal> findTradeById(String tradeId) {
        return Optional.ofNullable(activeTrades.get(tradeId));
    }

    public void removeTrade(String tradeId) {
        activeTrades.remove(tradeId);
    }
}
