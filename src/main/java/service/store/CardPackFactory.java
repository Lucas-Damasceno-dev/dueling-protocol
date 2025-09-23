package service.store;

import java.util.List;
import java.util.ArrayList;
import model.Card;
import model.CardPack;

public class CardPackFactory {
    public static CardPack createCardPack(String type) {
        if ("BASIC".equalsIgnoreCase(type)) {
            return new BasicCardPack();
        } else if ("RARE".equalsIgnoreCase(type)) {
            return new RareCardPack();
        }
        throw new IllegalArgumentException("Tipo de pacote desconhecido: " + type);
    }
}

// Implementações concretas
class BasicCardPack implements CardPack {
    @Override
    public String getName() { return "Pacote Básico"; }
    @Override
    public int getCost() { return 100; }
    @Override
    public List<Card> open() {
        // Lógica para gerar 5 cartas básicas aleatórias
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            cards.add(new Card("basic-" + i, "Carta Básica " + i, 1, 1, "Comum"));
        }
        return cards;
    }
}

class RareCardPack implements CardPack {
    @Override
    public String getName() { return "Pacote Raro"; }
    @Override
    public int getCost() { return 500; }
    @Override
    public List<Card> open() {
        // Lógica para gerar 5 cartas básicas aleatórias
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            cards.add(new Card("rare-" + i, "Carta Rara " + i, 3, 3, "Rara"));
        }
        return cards;
    }
}