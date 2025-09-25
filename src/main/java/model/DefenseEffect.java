package model;

public class DefenseEffect implements CardEffect {
    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        int defenseBonus = card.getDefense();
        caster.setBaseDefense(caster.getBaseDefense() + defenseBonus);
        System.out.println(caster.getNickname() + " usou '" + card.getName() + "', ganhando +" + defenseBonus + " de defesa base!");
    }
}