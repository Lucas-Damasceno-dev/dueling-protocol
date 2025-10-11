package model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class ComboEffect implements CardEffect, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ComboEffect.class);

    private final String requiredCardName;
    private final int bonusDamage;

    public ComboEffect(String requiredCardName, int bonusDamage) {
        this.requiredCardName = requiredCardName;
        this.bonusDamage = bonusDamage;
    }

    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        int totalDamage = card.getAttack();
        boolean comboActivated = session.getTurnManager().getPlayedCardsThisTurn().stream()
                .anyMatch(playedCard -> playedCard.getName().equals(requiredCardName));

        if (comboActivated) {
            totalDamage += bonusDamage;
            logger.info("COMBO ACTIVATED! {}'s {} gets +{} attack!", caster.getNickname(), card.getName(), bonusDamage);
        }

        int damage = Math.max(0, totalDamage - target.getBaseDefense());
        target.setHealthPoints(target.getHealthPoints() - damage);
    }
}
