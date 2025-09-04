package model;

public class MagicEffect implements CardEffect {
    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        int magicDamage = card.getAttack() * 2; // Magia pode ter um multiplicador, por exemplo
        target.setHealthPoints(target.getHealthPoints() - magicDamage);
        System.out.println(caster.getNickname() + " usou '" + card.getName() + "' em " + target.getNickname() + ", causando " + magicDamage + " de dano mágico!");
    }
}