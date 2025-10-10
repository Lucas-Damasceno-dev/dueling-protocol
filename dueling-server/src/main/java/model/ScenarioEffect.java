package model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the scenario effect for SCENARIO type cards.
 * This effect modifies the game scenario, affecting both players.
 * Currently, the effect grants a mana bonus to both players.
 */
import java.io.Serializable;
public class ScenarioEffect implements CardEffect, Serializable {
    private static final long serialVersionUID = 1L;
    
    private static final Logger logger = LoggerFactory.getLogger(ScenarioEffect.class);
    
    /**
     * {@inheritDoc}
     * Activates a new scenario in the game session. The scenario's duration is determined
     * by the card's defense value, and its per-turn damage by the card's attack value.
     *
     * @param session the game session where the effect will be applied
     * @param caster the player casting the card
     * @param target the target player of the card's effect (not used in this effect)
     * @param card the card being played
     */
    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        int duration = card.getDefense(); // Use defense for duration in turns
        session.setActiveScenario(card, duration);
        logger.info("{} played the scenario '{}', which will last for {} turns.", 
            caster.getNickname(), card.getName(), duration);
    }
}