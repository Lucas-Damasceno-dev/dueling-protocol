package model;

/**
 * Implementation of the equipment effect for EQUIPMENT type cards.
 * This effect increases the base attack of the player who plays the card.
 */
public class EquipmentEffect implements CardEffect {
    /**
     * {@inheritDoc}
     * Increases the base attack of the player who plays the card by the attack value of the card.
     *
     * @param session the game session where the effect will be applied
     * @param caster the player casting the card
     * @param target the target player of the card effect (not used in this effect)
     * @param card the card being played
     */
    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        int attackBonus = card.getAttack();
        caster.setBaseAttack(caster.getBaseAttack() + attackBonus);
        System.out.println(caster.getNickname() + " equipped '" + card.getName() + "', gaining +" + attackBonus + " base attack!");
    }
}