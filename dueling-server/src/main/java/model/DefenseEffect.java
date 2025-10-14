package model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the defense effect for DEFENSE-type cards.
 * This effect increases the casting player's base defense.
 */
public class DefenseEffect implements CardEffect {
    
    private static final Logger logger = LoggerFactory.getLogger(DefenseEffect.class);
    
    /**
     * {@inheritDoc}
     * Increases the casting player's base defense by the card's defense value.
     *
     * @param session the game session where the effect will be applied
     * @param caster the player casting the card
     * @param target the target player of the card's effect (not used in this effect)
     * @param card the card being played
     */
    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        int defenseBonus = card.getDefense();
        caster.setBaseDefense(caster.getBaseDefense() + defenseBonus);
        logger.info("{} used '{}', gaining +{} base defense!", caster.getNickname(), card.getName(), defenseBonus);
    }
}