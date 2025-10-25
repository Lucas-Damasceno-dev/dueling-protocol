package repository;

import model.Card;
import model.Card.CardType;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class CardRepository {
    private static final String CARD_STOCK_KEY = "card:stock";
    private final Map<String, Card> allCards = new HashMap<>();
    private final RMap<String, Integer> cardStock;
    private final SecureRandom random = new SecureRandom();

    private static final Logger logger = LoggerFactory.getLogger(CardRepository.class);

    public CardRepository(RedissonClient redissonClient) {
        this.cardStock = redissonClient.getMap(CARD_STOCK_KEY);

        try {
            // Inicializa os cards em memória (pode ser otimizado para ler de um DB)
            initializeCardDefinitions();

            // Inicializa o estoque no Redis apenas se estiver vazio
            if (cardStock.isEmpty()) {
                initializeStock();
            }

            logger.info("Card repository initialized with {} card types.", allCards.size());
        } catch (Exception e) {
            logger.error("Error initializing card repository: {}", e.getMessage(), e);
        }
    }

    private void initializeCardDefinitions() {
        allCards.put("basic-0", new Card("basic-0", "Basic Card 0", 1, 1, "Common", CardType.ATTACK, "Basic attack", 1));
        allCards.put("basic-1", new Card("basic-1", "Basic Card 1", 1, 1, "Common", CardType.ATTACK, "Basic attack", 1));
        allCards.put("basic-2", new Card("basic-2", "Basic Card 2", 1, 1, "Common", CardType.ATTACK, "Basic attack", 1));
        allCards.put("basic-3", new Card("basic-3", "Basic Card 3", 1, 1, "Common", CardType.ATTACK, "Basic attack", 1));
        allCards.put("basic-4", new Card("basic-4", "Basic Card 4", 1, 1, "Common", CardType.ATTACK, "Basic attack", 1));
        allCards.put("rare-0", new Card("rare-0", "Rare Card 0", 3, 3, "Rare", CardType.MAGIC, "Powerful magic", 2));
        allCards.put("rare-1", new Card("rare-1", "Rare Card 1", 3, 3, "Rare", CardType.MAGIC, "Powerful magic", 2));
        allCards.put("rare-2", new Card("rare-2", "Rare Card 2", 3, 3, "Rare", CardType.MAGIC, "Powerful magic", 2));
        allCards.put("rare-3", new Card("rare-3", "Rare Card 3", 3, 3, "Rare", CardType.MAGIC, "Powerful magic", 2));
        allCards.put("rare-4", new Card("rare-4", "Rare Card 4", 3, 3, "Rare", CardType.MAGIC, "Powerful magic", 2));
        allCards.put("equip-1", new Card("equip-1", "Light Sword", 2, 0, "Common", CardType.EQUIPMENT, "+2 attack for the bearer", 2));
        allCards.put("attrib-1", new Card("attrib-1", "Warrior's Fury", 3, 0, "Rare", CardType.ATTRIBUTE, "Increases base attack for 2 turns", 3));
        allCards.put("defense-1", new Card("defense-1", "Light Shield", 0, 2, "Common", CardType.DEFENSE, "+2 defense for the bearer", 2));
        allCards.put("scenario-1", new Card("scenario-1", "Battlefield", 0, 0, "Rare", CardType.SCENARIO, "Affects the battlefield", 3));
        allCards.put("legendary-1", new Card("legendary-1", "Ancestral Dragon", 10, 10, "Legendary", CardType.ATTACK, "Legendary attack", 10));
    }

    private void initializeStock() {
        Map<String, Integer> initialStock = new HashMap<>();
        allCards.keySet().forEach(id -> {
            if (id.startsWith("basic")) initialStock.put(id, 100);
            else if (id.startsWith("rare")) initialStock.put(id, 20);
            else if (id.startsWith("equip")) initialStock.put(id, 100);
            else if (id.startsWith("attrib")) initialStock.put(id, 20);
            else if (id.startsWith("defense")) initialStock.put(id, 100);
            else if (id.startsWith("scenario")) initialStock.put(id, 20);
            else if (id.startsWith("legendary")) initialStock.put(id, 5);
        });
        cardStock.putAll(initialStock);
        logger.info("Initialized Redis card stock with {} entries.", initialStock.size());
    }

    public Optional<Card> findById(String id) {
        return Optional.ofNullable(allCards.get(id));
    }

    public Map<String, Card> getAllCards() {
        return new HashMap<>(allCards);
    }

    public Optional<Card> claimCard(String id) {
        // Operação atômica para decrementar o estoque
        Integer newStock = cardStock.addAndGet(id, -1);

        if (newStock != null && newStock >= 0) {
            logger.info("Card {} claimed. Remaining stock: {}", id, newStock);
            return findById(id);
        } else {
            // Se o estoque ficou negativo, reverta a operação
            if (newStock != null) {
                cardStock.addAndGet(id, 1);
            }
            logger.warn("Attempt to claim card {}, but it is out of stock.", id);
            return Optional.empty();
        }
    }

    public Optional<Card> getRandomCardByRarity(String rarity) {
        List<String> availableCards = allCards.values().stream()
            .filter(c -> c.getRarity().equalsIgnoreCase(rarity) && cardStock.getOrDefault(c.getId(), 0) > 0)
            .map(Card::getId)
            .collect(Collectors.toList());

        if (availableCards.isEmpty()) {
            logger.warn("No cards of rarity {} available in stock.", rarity);
            return Optional.empty();
        }

        String randomCardId = availableCards.get(random.nextInt(availableCards.size()));
        // claimCard já é atômico e seguro para concorrência
        return claimCard(randomCardId);
    }
}
