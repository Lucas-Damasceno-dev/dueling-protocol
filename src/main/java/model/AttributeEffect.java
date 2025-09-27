package model;

/**
 * Implementation of the attribute effect for ATTRIBUTE-type cards.
 * This effect increases the casting player's base attributes,
 * including attack, defense, and mana.
 */
public class AttributeEffect implements CardEffect {
    /**
     * {@inheritDoc}
     * Increases the casting player's base attributes:
     * - Base attack is increased by the card's attack value
     * - Base defense is increased by the card's defense value
     * - Base mana is increased by half the card's mana cost
     *
     * @param session the game session where the effect will be applied
     * @param caster the player casting the card
     * @param target the target player of the card's effect (not used in this effect)
     * @param card the card being played
     */
    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        // Apply attribute bonuses based on card properties
        int attackBonus = card.getAttack();
        int defenseBonus = card.getDefense();
        int manaBonus = card.getManaCost() / 2; // Mana bonus is half the mana cost
        
        caster.setBaseAttack(caster.getBaseAttack() + attackBonus);
        caster.setBaseDefense(caster.getBaseDefense() + defenseBonus);
        caster.setBaseMana(caster.getBaseMana() + manaBonus);
        
        System.out.println(caster.getNickname() + " used '" + card.getName() + 
            "', gaining +" + attackBonus + " attack, +" + defenseBonus + 
            " defense and +" + manaBonus + " mana!");
    }
}