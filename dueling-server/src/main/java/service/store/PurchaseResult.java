package service.store;

import java.util.List;
import model.Card;

/**
 * Represents the result of a card pack purchase operation.
 * Contains the status of the purchase and, in case of success, the cards obtained.
 */
public class PurchaseResult {

    private final PurchaseStatus status;
    private final List<Card> cards;

    /**
     * Private constructor for PurchaseResult.
     *
     * @param status the status of the purchase
     * @param cards the list of cards obtained (null if purchase failed)
     */
    private PurchaseResult(PurchaseStatus status, List<Card> cards) {
        this.status = status;
        this.cards = cards;
    }

    /**
     * Creates a successful purchase result with the specified cards.
     *
     * @param cards the list of cards obtained from the purchase
     * @return a PurchaseResult indicating success
     */
    public static PurchaseResult success(List<Card> cards) {
        return new PurchaseResult(PurchaseStatus.SUCCESS, cards);
    }

    /**
     * Creates a failed purchase result with the specified status.
     *
     * @param status the status indicating why the purchase failed
     * @return a PurchaseResult indicating failure
     */
    public static PurchaseResult failure(PurchaseStatus status) {
        return new PurchaseResult(status, null);
    }

    /**
     * Gets the status of the purchase.
     *
     * @return the purchase status
     */
    public PurchaseStatus getStatus() {
        return status;
    }

    /**
     * Gets the list of cards obtained from the purchase.
     *
     * @return the list of cards obtained, or null if the purchase failed
     */
    public List<Card> getCards() {
        return cards;
    }

    /**
     * Checks if the purchase was successful.
     *
     * @return true if the purchase was successful, false otherwise
     */
    public boolean isSuccess() {
        return status == PurchaseStatus.SUCCESS;
    }

    /**
     * Enum representing the possible statuses of a card pack purchase.
     */
    public enum PurchaseStatus {
        /** Purchase completed successfully */
        SUCCESS,
        /** Player doesn't have enough coins for the purchase */
        INSUFFICIENT_FUNDS,
        /** The requested card pack is out of stock */
        OUT_OF_STOCK,
        /** The requested pack type was not found */
        PACK_NOT_FOUND,
        /** Server is busy and cannot handle the request right now */
        SERVER_BUSY,
        /** An internal server error occurred during the request */
        INTERNAL_SERVER_ERROR
    }
}
