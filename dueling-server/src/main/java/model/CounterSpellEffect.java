package model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class CounterSpellEffect implements CardEffect, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(CounterSpellEffect.class);

    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        // The logic for countering a spell will be handled in the GameSession
        logger.info("{} used {} to counter a spell!", caster.getNickname(), card.getName());
    }
}
