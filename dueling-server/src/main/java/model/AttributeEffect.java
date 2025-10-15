package model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the attribute effect for ATTRIBUTE-type cards.
 * This effect increases the casting player's base attributes,
 * including attack, defense, and mana.
 */
public class AttributeEffect implements CardEffect {
    
    private static final Logger logger = LoggerFactory.getLogger(AttributeEffect.class);
    
    /**
     * {@inheritDoc}
     * Applies a bonus to the caster's next attack in the current turn.
     * The bonus amount is determined by the card's attack value.
     *
     * @param session the game session where the effect will be applied
     * @param caster the player casting the card
     * @param target the target player of the card's effect (not used in this effect)
     * @param card the card being played
     */
    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        int attackBonus = card.getAttack();
        session.setNextAttackBonus(caster.getId(), attackBonus);
        
        logger.info("{} used '{}'. Their next attack this turn will have +{} bonus damage!", 
            caster.getNickname(), card.getName(), attackBonus);
    }
}