package repository;

import model.Card;
import model.Card.CardType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CardRepository {
    private static final Map<String, Card> allCards = new HashMap<>();
    
    private static final Logger logger = LoggerFactory.getLogger(CardRepository.class);

    static {
        try {
            // Basic attack cards
            for (int i = 0; i < 5; i++) {
                allCards.put("basic-" + i, new Card("basic-" + i, "Carta Básica " + i, 1, 1, "Comum", 
                CardType.ATTACK, "Ataque básico", 1));
            }

            // Rare magic cards
            for (int i = 0; i < 5; i++) {
                allCards.put("rare-" + i, new Card("rare-" + i, "Carta Rara " + i, 3, 3, "Rara", 
                CardType.MAGIC, "Magia poderosa", 2));
            }

            // Equipment cards
            allCards.put("equip-1", new Card("equip-1", "Espada Leve", 2, 0, "Comum", 
            CardType.EQUIPMENT, "+2 de ataque para o portador", 2));
            
            // Attribute cards
            allCards.put("attrib-1", new Card("attrib-1", "Fúria do Batalhador", 3, 0, "Rara", 
            CardType.ATTRIBUTE, "Aumenta o ataque base por 2 turnos", 3));
            
            // Defense cards
            allCards.put("defense-1", new Card("defense-1", "Escudo Leve", 0, 2, "Comum", 
            CardType.DEFENSE, "+2 de defesa para o portador", 2));
            
            // Scenario cards
            allCards.put("scenario-1", new Card("scenario-1", "Campo de Batalha", 0, 0, "Rara", 
            CardType.SCENARIO, "Afeta o campo de batalha", 3));
            
            // Legendary cards
            allCards.put("legendary-1", new Card("legendary-1", "Dragão Ancestral", 10, 10, "Lendária", 
            CardType.ATTACK, "Ataque lendário", 10));
            
            logger.info("Repositório de cartas inicializado com {} cartas", allCards.size());
        } catch (Exception e) {
            logger.error("Erro ao inicializar o repositório de cartas: {}", e.getMessage(), e);
        }
    }

    public static Optional<Card> findById(String id) {
        Card card = allCards.get(id);
        if (card == null) {
            logger.debug("Carta com ID {} não encontrada", id);
            return Optional.empty();
        }
        return Optional.of(card);
    }
}