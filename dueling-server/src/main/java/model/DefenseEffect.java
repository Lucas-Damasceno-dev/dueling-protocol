package model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Serializable;

/**
 * Implementation of the defense effect for DEFENSE-type cards.
 * This effect increases the casting player's base defense.
 */
public class DefenseEffect implements CardEffect, Serializable {
    private static final long serialVersionUID = 1L;
    
    private static final Logger logger = LoggerFactory.getLogger(DefenseEffect.class);

    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        int healAmount = card.getDefense();
        int previousHP = caster.getHealthPoints();
        caster.setHealthPoints(previousHP + healAmount);
        
        logger.info("{} used '{}', healing for {} points!", caster.getNickname(), card.getName(), healAmount);
        
        session.getGameFacade().notifyPlayers(
                java.util.Arrays.asList(caster.getId(), target.getId()),
                String.format("INFO:💚 %s usou '%s' e recuperou %d HP! (%d → %d HP)", 
                        caster.getNickname(), card.getName(), healAmount, previousHP, caster.getHealthPoints()));
    }
}