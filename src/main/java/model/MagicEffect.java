package model;

/**
 * Implementação do efeito mágico para cartas do tipo MAGIC.
 * Este efeito causa dano mágico ao jogador alvo, que é calculado como
 * o dobro do valor de ataque da carta.
 */
public class MagicEffect implements CardEffect {
    /**
     * {@inheritDoc}
     * Causa dano mágico ao jogador alvo igual ao dobro do valor de ataque da carta.
     *
     * @param session a sessão de jogo onde o efeito será aplicado
     * @param caster o jogador que está lançando a carta
     * @param target o jogador alvo do efeito da carta
     * @param card a carta que está sendo jogada
     */
    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        int magicDamage = card.getAttack() * 2; // Magia pode ter um multiplicador, por exemplo
        target.setHealthPoints(target.getHealthPoints() - magicDamage);
        System.out.println(caster.getNickname() + " usou '" + card.getName() + "' em " + target.getNickname() + ", causando " + magicDamage + " de dano mágico!");
    }
}