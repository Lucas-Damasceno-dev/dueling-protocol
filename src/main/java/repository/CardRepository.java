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

public class CardRepository {
    private static final Map<String, Card> allCards = new HashMap<>();
    private static final Map<String, Integer> cardStock = new ConcurrentHashMap<>();
    private static final SecureRandom random = new SecureRandom();
    
    private static final Logger logger = LoggerFactory.getLogger(CardRepository.class);

    static {
        try {
            // Basic attack cards
            for (int i = 0; i < 5; i++) {
                Card card = new Card("basic-" + i, "Carta Básica " + i, 1, 1, "Comum", CardType.ATTACK, "Ataque básico", 1);
                allCards.put(card.getId(), card);
                cardStock.put(card.getId(), 100); // Estoque alto para cartas comuns
            }

            // Rare magic cards
            for (int i = 0; i < 5; i++) {
                Card card = new Card("rare-" + i, "Carta Rara " + i, 3, 3, "Rara", CardType.MAGIC, "Magia poderosa", 2);
                allCards.put(card.getId(), card);
                cardStock.put(card.getId(), 20); // Estoque médio para cartas raras
            }

            // Equipment cards
            Card equipCard = new Card("equip-1", "Espada Leve", 2, 0, "Comum", CardType.EQUIPMENT, "+2 de ataque para o portador", 2);
            allCards.put(equipCard.getId(), equipCard);
            cardStock.put(equipCard.getId(), 100);

            // Attribute cards
            Card attribCard = new Card("attrib-1", "Fúria do Batalhador", 3, 0, "Rara", CardType.ATTRIBUTE, "Aumenta o ataque base por 2 turnos", 3);
            allCards.put(attribCard.getId(), attribCard);
            cardStock.put(attribCard.getId(), 20);

            // Defense cards
            Card defenseCard = new Card("defense-1", "Escudo Leve", 0, 2, "Comum", CardType.DEFENSE, "+2 de defesa para o portador", 2);
            allCards.put(defenseCard.getId(), defenseCard);
            cardStock.put(defenseCard.getId(), 100);

            // Scenario cards
            Card scenarioCard = new Card("scenario-1", "Campo de Batalha", 0, 0, "Rara", CardType.SCENARIO, "Afeta o campo de batalha", 3);
            allCards.put(scenarioCard.getId(), scenarioCard);
            cardStock.put(scenarioCard.getId(), 20);

            // Legendary cards
            Card legendaryCard = new Card("legendary-1", "Dragão Ancestral", 10, 10, "Lendária", CardType.ATTACK, "Ataque lendário", 10);
            allCards.put(legendaryCard.getId(), legendaryCard);
            cardStock.put(legendaryCard.getId(), 5); // Estoque baixo para cartas lendárias

            logger.info("Repositório de cartas inicializado com {} tipos de cartas e estoque total.", allCards.size());
        } catch (Exception e) {
            logger.error("Erro ao inicializar o repositório de cartas: {}", e.getMessage(), e);
        }
    }

    public static Optional<Card> findById(String id) {
        return Optional.ofNullable(allCards.get(id));
    }

    public static synchronized Optional<Card> claimCard(String id) {
        int stock = cardStock.getOrDefault(id, 0);
        if (stock > 0) {
            cardStock.put(id, stock - 1);
            logger.info("Carta {} reivindicada. Estoque restante: {}", id, stock - 1);
            return findById(id);
        }
        logger.warn("Tentativa de reivindicar a carta {}, mas está fora de estoque.", id);
        return Optional.empty();
    }

    public static synchronized Optional<Card> getRandomCardByRarity(String rarity) {
        List<String> availableCards = allCards.values().stream()
            .filter(c -> c.getRarity().equalsIgnoreCase(rarity) && cardStock.getOrDefault(c.getId(), 0) > 0)
            .map(Card::getId)
            .collect(Collectors.toList());

        if (availableCards.isEmpty()) {
            logger.warn("Nenhuma carta da raridade {} disponível no estoque.", rarity);
            return Optional.empty();
        }

        String randomCardId = availableCards.get(random.nextInt(availableCards.size()));
        return claimCard(randomCardId);
    }
}
