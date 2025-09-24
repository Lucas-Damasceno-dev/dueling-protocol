package model;

public class EquipmentEffect implements CardEffect {
    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        int attackBonus = card.getAttack();
        caster.setBaseAttack(caster.getBaseAttack() + attackBonus);
        System.out.println(caster.getNickname() + " equipou '" + card.getName() + "', ganhando +" + attackBonus + " de ataque base!");
    }
}