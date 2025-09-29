package service.store;

import model.Card;
import model.CardPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import repository.CardRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class CardPackFactory {

    private static final Logger logger = LoggerFactory.getLogger(CardPackFactory.class);
    private final CardRepository cardRepository;

    @Autowired
    public CardPackFactory(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public CardPack createCardPack(String type) {
        if ("BASIC".equalsIgnoreCase(type)) {
            return new BasicCardPack(cardRepository);
        } else if ("PREMIUM".equalsIgnoreCase(type)) {
            return new PremiumCardPack(cardRepository);
        } else if ("LEGENDARY".equalsIgnoreCase(type)) {
            return new LegendaryCardPack(cardRepository);
        }
        logger.warn("Unknown pack type: {}, using basic pack as default", type);
        return new BasicCardPack(cardRepository);
    }
}

class BasicCardPack implements CardPack {
    private final CardRepository cardRepository;
    public BasicCardPack(CardRepository cardRepository) { this.cardRepository = cardRepository; }

    @Override
    public String getName() { return "Basic Pack"; }
    @Override
    public int getCost() { return 100; }

    @Override
    public List<Card> open() {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            cardRepository.getRandomCardByRarity("Common").ifPresent(cards::add);
        }
        Collections.shuffle(cards);
        return cards;
    }
}

class PremiumCardPack implements CardPack {
    private final CardRepository cardRepository;
    public PremiumCardPack(CardRepository cardRepository) { this.cardRepository = cardRepository; }

    @Override
    public String getName() { return "Premium Pack"; }
    @Override
    public int getCost() { return 500; }

    @Override
    public List<Card> open() {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            cardRepository.getRandomCardByRarity("Rare").ifPresent(cards::add);
        }
        for (int i = 0; i < 2; i++) {
            cardRepository.getRandomCardByRarity("Common").ifPresent(cards::add);
        }
        Collections.shuffle(cards);
        return cards;
    }
}

class LegendaryCardPack implements CardPack {
    private final CardRepository cardRepository;
    public LegendaryCardPack(CardRepository cardRepository) { this.cardRepository = cardRepository; }

    @Override
    public String getName() { return "Legendary Pack"; }
    @Override
    public int getCost() { return 1500; }

    @Override
    public List<Card> open() {
        List<Card> cards = new ArrayList<>();
        cardRepository.getRandomCardByRarity("Legendary").ifPresent(cards::add);
        for (int i = 0; i < 2; i++) {
            cardRepository.getRandomCardByRarity("Rare").ifPresent(cards::add);
        }
        for (int i = 0; i < 2; i++) {
            cardRepository.getRandomCardByRarity("Common").ifPresent(cards::add);
        }
        Collections.shuffle(cards);
        return cards;
    }
}