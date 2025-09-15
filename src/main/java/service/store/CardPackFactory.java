package service.store;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import model.Card;
import model.CardPack;
import repository.CardRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class for creating card packs of different types.
 * This class provides a centralized way to create card packs based on their type.
 */
public class CardPackFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(CardPackFactory.class);
    
    /**
     * Creates a card pack of the specified type.
     * If the type is not recognized, a basic pack is created as default.
     *
     * @param type the type of card pack to create (e.g., "BASIC", "PREMIUM", "LEGENDARY")
     * @return a CardPack of the specified type
     */
    public static CardPack createCardPack(String type) {
        if ("BASIC".equalsIgnoreCase(type)) {
            return new BasicCardPack();
        } else if ("PREMIUM".equalsIgnoreCase(type)) {
            return new PremiumCardPack();
        } else if ("LEGENDARY".equalsIgnoreCase(type)) {
            return new LegendaryCardPack();
        }
        
        logger.warn("Unknown pack type: {}, using basic pack as default", type);
        return new BasicCardPack();
    }
}

/**
 * Implementation of a basic card pack.
 * Contains 5 common cards.
 */
class BasicCardPack implements CardPack {
    /**
     * {@inheritDoc}
     *
     * @return "Basic Pack"
     */
    @Override
    public String getName() { return "Basic Pack"; }
    
    /**
     * {@inheritDoc}
     *
     * @return 100 coins
     */
    @Override
    public int getCost() { return 100; }
    
    /**
     * {@inheritDoc}
     * Opens the pack and returns 5 common cards.
     *
     * @return a list of 5 common cards
     */
    @Override
    public List<Card> open() {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            CardRepository.getRandomCardByRarity("Common").ifPresent(cards::add);
        }
        Collections.shuffle(cards);
        return cards;
    }
}

/**
 * Implementation of a premium card pack.
 * Contains 3 rare cards and 2 common cards.
 */
class PremiumCardPack implements CardPack {
    /**
     * {@inheritDoc}
     *
     * @return "Premium Pack"
     */
    @Override
    public String getName() { return "Premium Pack"; }
    
    /**
     * {@inheritDoc}
     *
     * @return 500 coins
     */
    @Override
    public int getCost() { return 500; }
    
    /**
     * {@inheritDoc}
     * Opens the pack and returns 3 rare cards and 2 common cards.
     *
     * @return a list of 3 rare cards and 2 common cards
     */
    @Override
    public List<Card> open() {
        List<Card> cards = new ArrayList<>();
        // Add 3 rare cards and 2 basic cards
        for (int i = 0; i < 3; i++) {
            CardRepository.getRandomCardByRarity("Rare").ifPresent(cards::add);
        }
        for (int i = 0; i < 2; i++) {
            CardRepository.getRandomCardByRarity("Common").ifPresent(cards::add);
        }
        Collections.shuffle(cards);
        return cards;
    }
}

/**
 * Implementation of a legendary card pack.
 * Contains 1 legendary card (if available), 2 rare cards, and 2 common cards.
 */
class LegendaryCardPack implements CardPack {
    /**
     * {@inheritDoc}
     *
     * @return "Legendary Pack"
     */
    @Override
    public String getName() { return "Legendary Pack"; }
    
    /**
     * {@inheritDoc}
     *
     * @return 1500 coins
     */
    @Override
    public int getCost() { return 1500; }
    
    /**
     * {@inheritDoc}
     * Opens the pack and returns 1 legendary card (if available), 2 rare cards, and 2 common cards.
     *
     * @return a list containing 1 legendary card (if available), 2 rare cards, and 2 common cards
     */
    @Override
    public List<Card> open() {
        List<Card> cards = new ArrayList<>();
        
        // Add 1 legendary card (if available)
        CardRepository.getRandomCardByRarity("Legendary").ifPresent(cards::add);
        
        // Add 2 rare cards
        for (int i = 0; i < 2; i++) {
            CardRepository.getRandomCardByRarity("Rare").ifPresent(cards::add);
        }
        
        // Add 2 common cards (equipment/attribute are not rarities)
        for (int i = 0; i < 2; i++) {
            CardRepository.getRandomCardByRarity("Common").ifPresent(cards::add);
        }
        
        Collections.shuffle(cards);
        return cards;
    }
}
