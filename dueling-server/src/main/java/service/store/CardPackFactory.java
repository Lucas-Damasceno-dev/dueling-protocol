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
import java.util.Optional;

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
    
    public BasicCardPack(CardRepository cardRepository) { 
        this.cardRepository = cardRepository;
    }

    @Override
    public String getName() { return "Basic Pack"; }
    @Override
    public int getCost() { return 100; }

    @Override
    public List<Card> open() {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            // Try to get a Common card, fallback to any available card if out of stock
            Optional<Card> card = cardRepository.getRandomCardByRarity("Common");
            if (card.isPresent()) {
                cards.add(card.get());
            } else {
                // If Common cards are out of stock, get any card
                cardRepository.getRandomCard().ifPresent(cards::add);
            }
        }
        Collections.shuffle(cards);
        return cards;
    }
}

class PremiumCardPack implements CardPack {
    private final CardRepository cardRepository;
    
    public PremiumCardPack(CardRepository cardRepository) { 
        this.cardRepository = cardRepository;
    }

    @Override
    public String getName() { return "Premium Pack"; }
    @Override
    public int getCost() { return 500; }

    @Override
    public List<Card> open() {
        List<Card> cards = new ArrayList<>();
        
        // Try to get 3 Rare cards with fallback to any available card if out of stock
        for (int i = 0; i < 3; i++) {
            Optional<Card> card = cardRepository.getRandomCardByRarity("Rare");
            if (card.isPresent()) {
                cards.add(card.get());
            } else {
                // Fallback to any card if Rare is out of stock
                cardRepository.getRandomCard().ifPresent(cards::add);
            }
        }
        
        // Try to get 2 Common cards with fallback to any available card if out of stock
        for (int i = 0; i < 2; i++) {
            Optional<Card> card = cardRepository.getRandomCardByRarity("Common");
            if (card.isPresent()) {
                cards.add(card.get());
            } else {
                // Fallback to any card if Common is out of stock
                cardRepository.getRandomCard().ifPresent(cards::add);
            }
        }
        
        Collections.shuffle(cards);
        return cards;
    }
}

class LegendaryCardPack implements CardPack {
    private final CardRepository cardRepository;
    
    public LegendaryCardPack(CardRepository cardRepository) { 
        this.cardRepository = cardRepository;
    }

    @Override
    public String getName() { return "Legendary Pack"; }
    @Override
    public int getCost() { return 1500; }

    @Override
    public List<Card> open() {
        List<Card> cards = new ArrayList<>();
        
        // Try to get 1 Legendary card with fallback to any available card if out of stock
        Optional<Card> card = cardRepository.getRandomCardByRarity("Legendary");
        if (card.isPresent()) {
            cards.add(card.get());
        } else {
            // Fallback to any card if Legendary is out of stock
            cardRepository.getRandomCard().ifPresent(cards::add);
        }
        
        // Try to get 2 Rare cards with fallback to any available card if out of stock
        for (int i = 0; i < 2; i++) {
            Optional<Card> rareCard = cardRepository.getRandomCardByRarity("Rare");
            if (rareCard.isPresent()) {
                cards.add(rareCard.get());
            } else {
                // Fallback to any card if Rare is out of stock
                cardRepository.getRandomCard().ifPresent(cards::add);
            }
        }
        
        // Try to get 2 Common cards with fallback to any available card if out of stock
        for (int i = 0; i < 2; i++) {
            Optional<Card> commonCard = cardRepository.getRandomCardByRarity("Common");
            if (commonCard.isPresent()) {
                cards.add(commonCard.get());
            } else {
                // Fallback to any card if Common is out of stock
                cardRepository.getRandomCard().ifPresent(cards::add);
            }
        }
        
        Collections.shuffle(cards);
        return cards;
    }
}