package model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Serializable;

/**
 * Implementation of the magic effect for MAGIC-type cards.
 * This effect deals magical damage to the target player, calculated as
 * double the card's attack value.
 */
public class MagicEffect implements CardEffect, Serializable {
    private static final long serialVersionUID = 1L;
    
    private static final Logger logger = LoggerFactory.getLogger(MagicEffect.class);
    
    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        logger.info("{} used '{}', drawing a card!", caster.getNickname(), card.getName());
        session.drawCards(caster, 1);
        
        session.getGameFacade().notifyPlayers(
                java.util.Arrays.asList(caster.getId(), target.getId()),
                String.format("INFO:🔮 %s usou magia '%s' e comprou 1 carta!", 
                        caster.getNickname(), card.getName()));
    }
}