package service.store;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
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
        // Add 5 basic cards
        for (int i = 0; i < 5; i++) {
            CardRepository.findById("basic-" + i).ifPresent(cards::add);
        }
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
            CardRepository.findById("rare-" + i).ifPresent(cards::add);
        }
        for (int i = 0; i < 2; i++) {
            CardRepository.findById("basic-" + i).ifPresent(cards::add);
        }
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
        Random random = new Random();
        
        // Add 1 legendary card (if available)
        CardRepository.findById("legendary-1").ifPresent(cards::add);
        
        // Add 2 rare cards
        for (int i = 0; i < 2; i++) {
            CardRepository.findById("rare-" + i).ifPresent(cards::add);
        }
        
        // Add 2 equipment cards
        CardRepository.findById("equip-1").ifPresent(cards::add);
        CardRepository.findById("attrib-1").ifPresent(cards::add);
        
        return cards;
    }
}