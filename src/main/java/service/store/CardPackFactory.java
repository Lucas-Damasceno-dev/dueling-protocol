package service.store;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import model.Card;
import model.CardPack;
import repository.CardRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CardPackFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(CardPackFactory.class);
    
    public static CardPack createCardPack(String type) {
        if ("BASIC".equalsIgnoreCase(type)) {
            return new BasicCardPack();
        } else if ("PREMIUM".equalsIgnoreCase(type)) {
            return new PremiumCardPack();
        } else if ("LEGENDARY".equalsIgnoreCase(type)) {
            return new LegendaryCardPack();
        }
        
        logger.warn("Tipo de pacote desconhecido: {}, usando pacote básico como padrão", type);
        return new BasicCardPack();
    }
}

class BasicCardPack implements CardPack {
    @Override
    public String getName() { return "Pacote Básico"; }
    @Override
    public int getCost() { return 100; }
    @Override
    public List<Card> open() {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            CardRepository.getRandomCardByRarity("Comum").ifPresent(cards::add);
        }
        Collections.shuffle(cards);
        return cards;
    }
}

class PremiumCardPack implements CardPack {
    @Override
    public String getName() { return "Pacote Premium"; }
    @Override
    public int getCost() { return 500; }
    @Override
    public List<Card> open() {
        List<Card> cards = new ArrayList<>();
        // Add 3 rare cards and 2 basic cards
        for (int i = 0; i < 3; i++) {
            CardRepository.getRandomCardByRarity("Rara").ifPresent(cards::add);
        }
        for (int i = 0; i < 2; i++) {
            CardRepository.getRandomCardByRarity("Comum").ifPresent(cards::add);
        }
        Collections.shuffle(cards);
        return cards;
    }
}

class LegendaryCardPack implements CardPack {
    @Override
    public String getName() { return "Pacote Lendário"; }
    @Override
    public int getCost() { return 1500; }
    @Override
    public List<Card> open() {
        List<Card> cards = new ArrayList<>();
        
        // Add 1 legendary card (if available)
        CardRepository.getRandomCardByRarity("Lendária").ifPresent(cards::add);
        
        // Add 2 rare cards
        for (int i = 0; i < 2; i++) {
            CardRepository.getRandomCardByRarity("Rara").ifPresent(cards::add);
        }
        
        // Add 2 common cards (equipment/attribute are not rarities)
        for (int i = 0; i < 2; i++) {
            CardRepository.getRandomCardByRarity("Comum").ifPresent(cards::add);
        }
        
        Collections.shuffle(cards);
        return cards;
    }
}
