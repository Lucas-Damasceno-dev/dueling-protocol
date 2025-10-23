package model;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class TradeProposal implements Serializable {
    
    private static final long serialVersionUID = 1L;

    public enum Status {
        PENDING,
        ACCEPTED,
        REJECTED,
        COMPLETED,
        CANCELLED
    }

    private String tradeId;
    private String proposingPlayerId;
    private String targetPlayerId;
    private List<String> offeredCardIds;
    private List<String> requestedCardIds;
    private Status status;

    // Default constructor for Jackson
    public TradeProposal() {
        this.tradeId = UUID.randomUUID().toString();
        this.status = Status.PENDING;
    }

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
    public void setTradeId(String tradeId) { this.tradeId = tradeId; }
    
    public String getProposingPlayerId() { return proposingPlayerId; }
    public void setProposingPlayerId(String proposingPlayerId) { this.proposingPlayerId = proposingPlayerId; }
    
    public String getTargetPlayerId() { return targetPlayerId; }
    public void setTargetPlayerId(String targetPlayerId) { this.targetPlayerId = targetPlayerId; }
    
    public List<String> getOfferedCardIds() { return offeredCardIds; }
    public void setOfferedCardIds(List<String> offeredCardIds) { this.offeredCardIds = offeredCardIds; }
    
    public List<String> getRequestedCardIds() { return requestedCardIds; }
    public void setRequestedCardIds(List<String> requestedCardIds) { this.requestedCardIds = requestedCardIds; }
    
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
