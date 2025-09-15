package model;

/**
 * Implementation of the scenario effect for SCENARIO type cards.
 * This effect modifies the game scenario, affecting both players.
 * Currently, the effect grants a mana bonus to both players.
 */
public class ScenarioEffect implements CardEffect {
    /**
     * {@inheritDoc}
     * Applies a scenario effect that grants a mana bonus to both players.
     *
     * @param session the game session where the effect will be applied
     * @param caster the player casting the card
     * @param target the target player of the card effect
     * @param card the card being played
     */
    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        // Apply scenario effects - could modify game state or provide special conditions
        int turnBonus = 2; // Bonus turns or actions
        
        // For now, we'll add a bonus to both players' mana as a scenario effect
        caster.setBaseMana(caster.getBaseMana() + turnBonus);
        target.setBaseMana(target.getBaseMana() + turnBonus);
        
        System.out.println("The scenario '" + card.getName() + 
            "' affected the battlefield! Both players gained +" + turnBonus + " mana!");
    }
}