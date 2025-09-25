package model;

public class ScenarioEffect implements CardEffect {
    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        // Apply scenario effects - could modify game state or provide special conditions
        int turnBonus = 2; // Bonus turns or actions
        
        // For now, we'll add a bonus to both players' mana as a scenario effect
        caster.setBaseMana(caster.getBaseMana() + turnBonus);
        target.setBaseMana(target.getBaseMana() + turnBonus);
        
        System.out.println("O cenário '" + card.getName() + 
            "' afetou o campo de batalha! Ambos os jogadores ganharam +" + turnBonus + " mana!");
    }
}