package model;

import java.util.List;

/**
 * Interface for card packs in the game store.
 * Defines the contract for card packs that players can purchase to obtain new cards.
 */
public interface CardPack {
    /**
     * Gets the name of the card pack.
     *
     * @return the name of the card pack
     */
    String getName();
    
    /**
     * Gets the cost of the card pack in coins.
     *
     * @return the cost of the card pack
     */
    int getCost();
    
    /**
     * Opens the card pack and returns the cards inside.
     * This method consumes the pack and returns the cards obtained.
     *
     * @return a list of cards obtained from opening the pack
     */
    List<Card> open();
}