package model.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.Card;
import model.CardEffect;
import model.AttackEffect;
import model.DefenseEffect;
import model.MagicEffect;
import model.AttributeEffect;
import model.ScenarioEffect;
import model.EquipmentEffect;

import java.io.Serializable;
public class CardEffectService implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(CardEffectService.class);

    public CardEffect getCardEffect(Card card) {
        switch (card.getCardType()) {
            case ATTACK: return new AttackEffect();
            case DEFENSE: return new DefenseEffect();
            case MAGIC: return new MagicEffect();
            case ATTRIBUTE: return new AttributeEffect();
            case SCENARIO: return new ScenarioEffect();
            case EQUIPMENT: return new EquipmentEffect();
            default: logger.warn("Effect for {} not implemented", card.getCardType()); return null;
        }
    }
}
