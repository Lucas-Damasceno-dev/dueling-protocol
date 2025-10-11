package repository;

import model.Card;
import model.Card.CardType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class CardRepository {
    private final Map<String, Card> allCards = new HashMap<>();
    private final Map<String, Integer> cardStock = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    
    private static final Logger logger = LoggerFactory.getLogger(CardRepository.class);

    public CardRepository() {
        try {
            for (int i = 0; i < 5; i++) {
                Card card = new Card("basic-" + i, "Basic Card " + i, 1, 1, "Common", CardType.ATTACK, "Basic attack", 1);
                allCards.put(card.getId(), card);
                cardStock.put(card.getId(), 100);
            }

            for (int i = 0; i < 5; i++) {
                Card card = new Card("rare-" + i, "Rare Card " + i, 3, 3, "Rare", CardType.MAGIC, "Powerful magic", 2);
                allCards.put(card.getId(), card);
                cardStock.put(card.getId(), 20);
            }

            Card equipCard = new Card("equip-1", "Light Sword", 2, 0, "Common", CardType.EQUIPMENT, "+2 attack for the bearer", 2);
            allCards.put(equipCard.getId(), equipCard);
            cardStock.put(equipCard.getId(), 100);

            Card attribCard = new Card("attrib-1", "Warrior's Fury", 3, 0, "Rare", CardType.ATTRIBUTE, "Increases base attack for 2 turns", 3);
            allCards.put(attribCard.getId(), attribCard);
            cardStock.put(attribCard.getId(), 20);

            Card defenseCard = new Card("defense-1", "Light Shield", 0, 2, "Common", CardType.DEFENSE, "+2 defense for the bearer", 2);
            allCards.put(defenseCard.getId(), defenseCard);
            cardStock.put(defenseCard.getId(), 100);

            Card scenarioCard = new Card("scenario-1", "Battlefield", 0, 0, "Rare", CardType.SCENARIO, "Affects the battlefield", 3);
            allCards.put(scenarioCard.getId(), scenarioCard);
            cardStock.put(scenarioCard.getId(), 20);

            Card legendaryCard = new Card("legendary-1", "Ancestral Dragon", 10, 10, "Legendary", CardType.ATTACK, "Legendary attack", 10);
            allCards.put(legendaryCard.getId(), legendaryCard);
            cardStock.put(legendaryCard.getId(), 5);

            logger.info("Card repository initialized with {} card types and total stock.", allCards.size());
        } catch (Exception e) {
            logger.error("Error initializing card repository: {}", e.getMessage(), e);
        }
    }

    public Optional<Card> findById(String id) {
        return Optional.ofNullable(allCards.get(id));
    }

    public synchronized Optional<Card> claimCard(String id) {
        int stock = cardStock.getOrDefault(id, 0);
        if (stock > 0) {
            cardStock.put(id, stock - 1);
            logger.info("Card {} claimed. Remaining stock: {}", id, stock - 1);
            return findById(id);
        }
        logger.warn("Attempt to claim card {}, but it is out of stock.", id);
        return Optional.empty();
    }

    public synchronized Optional<Card> getRandomCardByRarity(String rarity) {
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