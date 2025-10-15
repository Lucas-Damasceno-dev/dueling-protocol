package model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the defense effect for DEFENSE-type cards.
 * This effect increases the casting player's base defense.
 */
import java.io.Serializable;
public class DefenseEffect implements CardEffect, Serializable {
    private static final long serialVersionUID = 1L;
    
    private static final Logger logger = LoggerFactory.getLogger(DefenseEffect.class);

    /**
     * {@inheritDoc}
     * Heals the caster by an amount equal to the card's defense value.
     *
     * @param session the game session where the effect will be applied
     * @param caster the player casting the card
     * @param target the target player of the card's effect (not used in this effect)
     * @param card the card being played
     */
    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        int healAmount = card.getDefense();
        // Note: In the future, we could cap healing at a max health value.
        caster.setHealthPoints(caster.getHealthPoints() + healAmount);
        logger.info("{} used '{}', healing for {} points!", caster.getNickname(), card.getName(), healAmount);
    }
}