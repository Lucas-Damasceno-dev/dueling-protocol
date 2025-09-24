package model;

public class AttackEffect implements CardEffect {
    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        int dano = card.getAttack();
        target.setHealthPoints(target.getHealthPoints() - dano);
    }
}
