package repository;

import model.Card;
import model.Card.CardType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CardRepository {
    private static final Map<String, Card> allCards = new HashMap<>();

    static {
        for (int i = 0; i < 5; i++) {
            allCards.put("basic-" + i, new Card("basic-" + i, "Carta Básica " + i, 1, 1, "Comum", 
            CardType.ATTACK, "Ataque básico", 1));
        }

        for (int i = 0; i < 5; i++) {
            allCards.put("rare-" + i, new Card("rare-" + i, "Carta Rara " + i, 3, 3, "Rara", 
            CardType.MAGIC, "Magia poderosa", 2));
        }

        allCards.put("equip-1", new Card("equip-1", "Espada Leve", 2, 0, "Comum", 
        CardType.EQUIPMENT, "+2 de ataque para o portador", 2));
        allCards.put("attrib-1", new Card("attrib-1", "Fúria do Batalhador", 3, 0, "Rara", 
        CardType.ATTRIBUTE, "Aumenta o ataque base por 2 turnos", 3));
    }

    public static Optional<Card> findById(String id) {
        return Optional.ofNullable(allCards.get(id));
    }
}