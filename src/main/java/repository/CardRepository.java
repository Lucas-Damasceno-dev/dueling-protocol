package repository;

import model.Card;
import model.Card.CardType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.Random;
import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository for card data and card stock management.
 * This class manages all available cards in the game and their stock levels.
 * It provides methods to find cards by ID, claim cards from stock, and get random cards by rarity.
 */
public class CardRepository {
    private static final Map<String, Card> allCards = new HashMap<>();
    private static final Map<String, Integer> cardStock = new ConcurrentHashMap<>();
    private static final SecureRandom random = new SecureRandom();
    
    private static final Logger logger = LoggerFactory.getLogger(CardRepository.class);

    static {
        try {
            // Basic attack cards
            for (int i = 0; i < 5; i++) {
                Card card = new Card("basic-" + i, "Basic Card " + i, 1, 1, "Common", CardType.ATTACK, "Basic attack", 1);
                allCards.put(card.getId(), card);
                cardStock.put(card.getId(), 100); // High stock for common cards
            }

            // Rare magic cards
            for (int i = 0; i < 5; i++) {
                Card card = new Card("rare-" + i, "Rare Card " + i, 3, 3, "Rare", CardType.MAGIC, "Powerful magic", 2);
                allCards.put(card.getId(), card);
                cardStock.put(card.getId(), 20); // Medium stock for rare cards
            }

            // Equipment cards
            Card equipCard = new Card("equip-1", "Light Sword", 2, 0, "Common", CardType.EQUIPMENT, "+2 attack for the bearer", 2);
            allCards.put(equipCard.getId(), equipCard);
            cardStock.put(equipCard.getId(), 100);

            // Attribute cards
            Card attribCard = new Card("attrib-1", "Warrior's Fury", 3, 0, "Rare", CardType.ATTRIBUTE, "Increases base attack for 2 turns", 3);
            allCards.put(attribCard.getId(), attribCard);
            cardStock.put(attribCard.getId(), 20);

            // Defense cards
            Card defenseCard = new Card("defense-1", "Light Shield", 0, 2, "Common", CardType.DEFENSE, "+2 defense for the bearer", 2);
            allCards.put(defenseCard.getId(), defenseCard);
            cardStock.put(defenseCard.getId(), 100);

            // Scenario cards
            Card scenarioCard = new Card("scenario-1", "Battlefield", 0, 0, "Rare", CardType.SCENARIO, "Affects the battlefield", 3);
            allCards.put(scenarioCard.getId(), scenarioCard);
            cardStock.put(scenarioCard.getId(), 20);

            // Legendary cards
            Card legendaryCard = new Card("legendary-1", "Ancestral Dragon", 10, 10, "Legendary", CardType.ATTACK, "Legendary attack", 10);
            allCards.put(legendaryCard.getId(), legendaryCard);
            cardStock.put(legendaryCard.getId(), 5); // Low stock for legendary cards

            logger.info("Card repository initialized with {} card types and total stock.", allCards.size());
        } catch (Exception e) {
            logger.error("Error initializing card repository: {}", e.getMessage(), e);
        }
    }

    /**
     * Finds a card by its unique identifier.
     *
     * @param id the unique identifier of the card
     * @return an Optional containing the card if found, or empty if not found
     */
    public static Optional<Card> findById(String id) {
        return Optional.ofNullable(allCards.get(id));
    }

    /**
     * Claims a card from stock, reducing its availability.
     * This method is synchronized to prevent race conditions when multiple players
     * try to claim the same card simultaneously.
     *
     * @param id the unique identifier of the card to claim
     * @return an Optional containing the card if successfully claimed, or empty if out of stock
     */
    public static synchronized Optional<Card> claimCard(String id) {
        int stock = cardStock.getOrDefault(id, 0);
        if (stock > 0) {
            cardStock.put(id, stock - 1);
            logger.info("Card {} claimed. Remaining stock: {}", id, stock - 1);
            return findById(id);
        }
        logger.warn("Attempt to claim card {}, but it is out of stock.", id);
        return Optional.empty();
    }

    /**
     * Gets a random card of a specific rarity that is still in stock.
     * The card is automatically claimed from stock when retrieved.
     *
     * @param rarity the rarity of the card to retrieve (e.g., "Common", "Rare", "Legendary")
     * @return an Optional containing a random card of the specified rarity, or empty if none available
     */
    public static synchronized Optional<Card> getRandomCardByRarity(String rarity) {
        List<String> availableCards = allCards.values().stream()
            .filter(c -> c.getRarity().equalsIgnoreCase(rarity) && cardStock.getOrDefault(c.getId(), 0) > 0)
            .map(Card::getId)
            .collect(Collectors.toList());

        if (availableCards.isEmpty()) {
            logger.warn("No cards of rarity {} available in stock.", rarity);
            return Optional.empty();
        }

        String randomCardId = availableCards.get(random.nextInt(availableCards.size()));
        return claimCard(randomCardId);
    }
}
