package model;

/**
 * Implementation of the attack effect for ATTACK-type cards.
 * This effect deals damage to the target player equal to the card's attack value.
 */
import java.io.Serializable;
public class AttackEffect implements CardEffect, Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * {@inheritDoc}
     * Deals damage to the target player equal to the card's attack value.
     *
     * @param session the game session where the effect will be applied
     * @param caster the player casting the card
     * @param target the target player of the card's effect
     * @param card the card being played
     */
    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        int bonus = session.consumeNextAttackBonus(caster.getId());
        int damage = Math.max(0, card.getAttack() + bonus - target.getBaseDefense());
        target.setHealthPoints(target.getHealthPoints() - damage);
    }
}
