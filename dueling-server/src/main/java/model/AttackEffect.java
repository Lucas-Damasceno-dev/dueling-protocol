package model;

/**
 * Implementation of the attack effect for ATTACK-type cards.
 * This effect deals damage to the target player equal to the card's attack value.
 */
public class AttackEffect implements CardEffect {
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
        int damage = Math.max(0, card.getAttack() - target.getBaseDefense());
        target.setHealthPoints(target.getHealthPoints() - damage);
    }
}
