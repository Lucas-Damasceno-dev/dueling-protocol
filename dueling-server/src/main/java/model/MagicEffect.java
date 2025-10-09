package model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the magic effect for MAGIC-type cards.
 * This effect deals magical damage to the target player, calculated as
 * double the card's attack value.
 */
public class MagicEffect implements CardEffect {
    
    private static final Logger logger = LoggerFactory.getLogger(MagicEffect.class);
    
    /**
     * {@inheritDoc}
     * Deals magical damage to the target player equal to double the card's attack value.
     *
     * @param session the game session where the effect will be applied
     * @param caster the player casting the card
     * @param target the target player of the card's effect
     * @param card the card being played
     */
    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        int magicDamage = card.getAttack() * 2; // Magic can have a multiplier, for example
        target.setHealthPoints(target.getHealthPoints() - magicDamage);
        logger.info("{} used '{}' on {}, dealing {} magical damage!", caster.getNickname(), card.getName(), target.getNickname(), magicDamage);
    }
}