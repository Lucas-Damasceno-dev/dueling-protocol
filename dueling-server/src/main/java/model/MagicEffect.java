package model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the magic effect for MAGIC-type cards.
 * This effect deals magical damage to the target player, calculated as
 * double the card's attack value.
 */
import java.io.Serializable;
public class MagicEffect implements CardEffect, Serializable {
    private static final long serialVersionUID = 1L;
    
    private static final Logger logger = LoggerFactory.getLogger(MagicEffect.class);
    
    /**
     * {@inheritDoc}
     * The caster draws a single card from their deck.
     *
     * @param session the game session where the effect will be applied
     * @param caster the player casting the card
     * @param target the target player of the card's effect (not used in this effect)
     * @param card the card being played
     */
    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        logger.info("{} used '{}', drawing a card!", caster.getNickname(), card.getName());
        session.drawCards(caster, 1);
    }