package service.store;

import java.util.List;
import model.Card;

public class PurchaseResult {

    private final PurchaseStatus status;
    private final List<Card> cards;

    private PurchaseResult(PurchaseStatus status, List<Card> cards) {
        this.status = status;
        this.cards = cards;
    }

    public static PurchaseResult success(List<Card> cards) {
        return new PurchaseResult(PurchaseStatus.SUCCESS, cards);
    }

    public static PurchaseResult failure(PurchaseStatus status) {
        return new PurchaseResult(status, null);
    }

    public PurchaseStatus getStatus() {
        return status;
    }

    public List<Card> getCards() {
        return cards;
    }

    public boolean isSuccess() {
        return status == PurchaseStatus.SUCCESS;
    }

    public enum PurchaseStatus {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        OUT_OF_STOCK,
        PACK_NOT_FOUND
    }
}
