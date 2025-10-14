package model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the equipment effect for EQUIPMENT type cards.
 * This effect increases the base attack of the player who plays the card.
 */
public class EquipmentEffect implements CardEffect {
    
    private static final Logger logger = LoggerFactory.getLogger(EquipmentEffect.class);
    
    /**
     * {@inheritDoc}
     * Increases the base attack of the player who plays the card by the attack value of the card.
     *
     * @param session the game session where the effect will be applied
     * @param caster the player casting the card
     * @param target the target player of the card effect (not used in this effect)
     * @param card the card being played
     */
    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        int attackBonus = card.getAttack();
        caster.setBaseAttack(caster.getBaseAttack() + attackBonus);
        logger.info("{} equipped '{}', gaining +{} base attack!", caster.getNickname(), card.getName(), attackBonus);
    }
}