package repository;

import model.Card;
import model.Card.CardType;
import org.redisson.api.RAtomicLong;
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
    private static final String CARD_STOCK_PREFIX = "card:stock:";
    private static final String STOCK_INIT_FLAG = "card:stock:initialized";
    private final Map<String, Card> allCards = new HashMap<>();
    private final SecureRandom random = new SecureRandom();

    private final RedissonClient redissonClient;
    private static final Logger logger = LoggerFactory.getLogger(CardRepository.class);

    public CardRepository(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;

        try {
            // Inicializa os cards em memória (pode ser otimizado para ler de um DB)
            initializeCardDefinitions();

            // Inicializa o estoque no Redis apenas se não foi inicializado antes
            RAtomicLong initFlag = redissonClient.getAtomicLong(STOCK_INIT_FLAG);
            if (initFlag.get() == 0) {
                initializeStock();
                initFlag.set(1);
            } else {
                // Se o estoque foi inicializado, verificar se está muito baixo e resetar se necessário
                resetStockIfDepleted();
            }

            logger.info("Card repository initialized with {} card types.", allCards.size());
        } catch (Exception e) {
            logger.error("Error initializing card repository: {}", e.getMessage(), e);
        }
    }
    
    private RAtomicLong getStockCounter(String cardId) {
        return redissonClient.getAtomicLong(CARD_STOCK_PREFIX + cardId);
    }

    private void initializeCardDefinitions() {
        allCards.put("basic-0", new Card("basic-0", "Basic Card 0", 50, 1, "Common", CardType.ATTACK, "Basic attack", 1));
        allCards.put("basic-1", new Card("basic-1", "Basic Card 1", 50, 1, "Common", CardType.ATTACK, "Basic attack", 1));
        allCards.put("basic-2", new Card("basic-2", "Basic Card 2", 50, 1, "Common", CardType.ATTACK, "Basic attack", 1));
        allCards.put("basic-3", new Card("basic-3", "Basic Card 3", 50, 1, "Common", CardType.ATTACK, "Basic attack", 1));
        allCards.put("basic-4", new Card("basic-4", "Basic Card 4", 50, 1, "Common", CardType.ATTACK, "Basic attack", 1));
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
        allCards.keySet().forEach(id -> {
            int stock = 0;
            if (id.startsWith("basic")) stock = 100;
            else if (id.startsWith("rare")) stock = 20;
            else if (id.startsWith("equip")) stock = 100;
            else if (id.startsWith("attrib")) stock = 20;
            else if (id.startsWith("defense")) stock = 100;
            else if (id.startsWith("scenario")) stock = 20;
            else if (id.startsWith("legendary")) stock = 5;
            
            getStockCounter(id).set(stock);
        });
        logger.info("Initialized Redis card stock with {} entries using RAtomicLong.", allCards.size());
    }
    
    public void resetStockIfDepleted() {
        // Check if all or most cards are out of stock
        long nonZeroStockCount = allCards.keySet().stream()
            .filter(id -> getStockCounter(id).get() > 0)
            .count();
        
        logger.info("Checking stock status: {} cards with stock > 0 out of {} total card types", nonZeroStockCount, allCards.size());
        
        // If very few cards are available (less than 25% of total card types), reset the stock
        if (nonZeroStockCount < allCards.size() * 0.25) {
            logger.info("Low stock detected ({} cards available), resetting card stock", nonZeroStockCount);
            initializeStock();
            // Re-check after reset
            long newNonZeroStockCount = allCards.keySet().stream()
                .filter(id -> getStockCounter(id).get() > 0)
                .count();
            logger.info("After reset: {} cards with stock > 0", newNonZeroStockCount);
        } else {
            logger.info("Stock level acceptable, no reset needed");
        }
    }

    public Optional<Card> findById(String id) {
        return Optional.ofNullable(allCards.get(id));
    }

    public Map<String, Card> getAllCards() {
        return new HashMap<>(allCards);
    }

    public Optional<Card> claimCard(String id) {
        try {
            RAtomicLong counter = getStockCounter(id);
            long newStock = counter.decrementAndGet();
            
            if (newStock < 0) {
                // Revert the decrement
                counter.incrementAndGet();
                logger.warn("Attempt to claim card {}, but it is out of stock", id);
                return Optional.empty();
            }
            
            logger.info("Card {} claimed atomically. New stock: {}", id, newStock);
            
            // Create a unique copy of the card with a unique ID
            Card template = allCards.get(id);
            if (template == null) {
                return Optional.empty();
            }
            
            // Generate unique ID using template name + short UUID
            // Format: basic-0-abc123, combo-1-def456, etc.
            String uniqueSuffix = java.util.UUID.randomUUID().toString().substring(0, 6);
            String uniqueId = id + "-" + uniqueSuffix;
            Card uniqueCard = new Card(
                uniqueId,
                template.getName(),
                template.getAttack(),
                template.getDefense(),
                template.getRarity(),
                template.getCardType(),
                template.getEffectDescription(),
                template.getManaCost(),
                template.getEffectParameters()
            );
            
            logger.debug("Created unique card instance: {} (template: {})", uniqueId, id);
            return Optional.of(uniqueCard);
            
        } catch (Exception e) {
            logger.error("Error claiming card {}: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<Card> getRandomCardByRarity(String rarity) {
        List<String> availableCards = allCards.values().stream()
            .filter(c -> c.getRarity().equalsIgnoreCase(rarity) && getStockCounter(c.getId()).get() > 0)
            .map(Card::getId)
            .collect(Collectors.toList());

        if (availableCards.isEmpty()) {
            logger.warn("No cards of rarity {} available in stock. Attempting to reset stock.", rarity);
            // Try to reset stock if it's depleted
            resetStockIfDepleted();
            
            // After resetting, check again
            availableCards = allCards.values().stream()
                .filter(c -> c.getRarity().equalsIgnoreCase(rarity) && getStockCounter(c.getId()).get() > 0)
                .map(Card::getId)
                .collect(Collectors.toList());

            if (availableCards.isEmpty()) {
                logger.error("No cards of rarity {} available even after stock reset.", rarity);
                return Optional.empty();
            }
        }

        String randomCardId = availableCards.get(random.nextInt(availableCards.size()));
        // claimCard already is atomic and thread-safe
        return claimCard(randomCardId);
    }
    
    public Optional<Card> getRandomCard() {
        List<String> availableCards = allCards.values().stream()
            .filter(c -> getStockCounter(c.getId()).get() > 0)
            .map(Card::getId)
            .collect(Collectors.toList());

        if (availableCards.isEmpty()) {
            logger.warn("No cards available in stock. Attempting to reset stock.");
            // Try to reset stock if it's depleted
            resetStockIfDepleted();
            
            // After resetting, check again
            availableCards = allCards.values().stream()
                .filter(c -> getStockCounter(c.getId()).get() > 0)
                .map(Card::getId)
                .collect(Collectors.toList());

            if (availableCards.isEmpty()) {
                logger.error("No cards available even after stock reset.");
                return Optional.empty();
            }
        }

        String randomCardId = availableCards.get(random.nextInt(availableCards.size()));
        // claimCard already is atomic and thread-safe
        return claimCard(randomCardId);
    }
    
    // Method for testing: set stock for a specific card
    public void setStock(String cardId, int stock) {
        getStockCounter(cardId).set(stock);
        logger.info("Set stock for card {} to {}", cardId, stock);
    }
    
    // Method for testing: clear all stock
    public void clearStock() {
        RAtomicLong initFlag = redissonClient.getAtomicLong(STOCK_INIT_FLAG);
        initFlag.set(0);
        allCards.keySet().forEach(id -> getStockCounter(id).set(0));
        logger.info("Cleared all card stock");
    }
}
