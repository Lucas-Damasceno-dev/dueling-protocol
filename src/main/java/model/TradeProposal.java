package model;

import java.util.List;
import java.util.UUID;

public class TradeProposal {

    public enum Status {
        PENDING,
        ACCEPTED,
        REJECTED,
        COMPLETED,
        CANCELLED
    }

    private final String tradeId;
    private final String proposingPlayerId;
    private final String targetPlayerId;
    private final List<String> offeredCardIds;
    private final List<String> requestedCardIds;
    private Status status;

    public TradeProposal(String proposingPlayerId, String targetPlayerId, List<String> offeredCardIds, List<String> requestedCardIds) {
        this.tradeId = UUID.randomUUID().toString();
        this.proposingPlayerId = proposingPlayerId;
        this.targetPlayerId = targetPlayerId;
        this.offeredCardIds = offeredCardIds;
        this.requestedCardIds = requestedCardIds;
        this.status = Status.PENDING;
    }

    // Getters and Setters
    public String getTradeId() { return tradeId; }
    public String getProposingPlayerId() { return proposingPlayerId; }
    public String getTargetPlayerId() { return targetPlayerId; }
    public List<String> getOfferedCardIds() { return offeredCardIds; }
    public List<String> getRequestedCardIds() { return requestedCardIds; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
