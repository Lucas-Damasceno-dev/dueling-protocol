package model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the scenario effect for SCENARIO type cards.
 * This effect modifies the game scenario, affecting both players.
 * Currently, the effect grants a mana bonus to both players.
 */
public class ScenarioEffect implements CardEffect {
    
    private static final Logger logger = LoggerFactory.getLogger(ScenarioEffect.class);
    
    /**
     * {@inheritDoc}
     * Applies a scenario effect that grants a mana bonus to both players.
     *
     * @param session the game session where the effect will be applied
     * @param caster the player casting the card
     * @param target the target player of the card effect
     * @param card the card being played
     */
    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        // Apply scenario effects - could modify game state or provide special conditions
        int turnBonus = 2; // Bonus turns or actions
        
        // For now, we'll add a bonus to both players' mana as a scenario effect
        caster.setBaseMana(caster.getBaseMana() + turnBonus);
        target.setBaseMana(target.getBaseMana() + turnBonus);
        
        logger.info("The scenario '{}' affected the battlefield! Both players gained +{} mana!", 
            card.getName(), turnBonus);
    }
}