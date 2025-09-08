package model;

public class AttributeEffect implements CardEffect {
    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        // Apply attribute bonuses based on card properties
        int attackBonus = card.getAttack();
        int defenseBonus = card.getDefense();
        int manaBonus = card.getManaCost() / 2; // Mana bonus is half the mana cost
        
        caster.setBaseAttack(caster.getBaseAttack() + attackBonus);
        caster.setBaseDefense(caster.getBaseDefense() + defenseBonus);
        caster.setBaseMana(caster.getBaseMana() + manaBonus);
        
        System.out.println(caster.getNickname() + " usou '" + card.getName() + 
            "', ganhando +" + attackBonus + " ataque, +" + defenseBonus + 
            " defesa e +" + manaBonus + " mana!");
    }
}