package model;

/**
 * Implementação do efeito de ataque para cartas do tipo ATTACK.
 * Este efeito causa dano ao jogador alvo igual ao valor de ataque da carta.
 */
public class AttackEffect implements CardEffect {
    /**
     * {@inheritDoc}
     * Causa dano ao jogador alvo igual ao valor de ataque da carta.
     *
     * @param session a sessão de jogo onde o efeito será aplicado
     * @param caster o jogador que está lançando a carta
     * @param target o jogador alvo do efeito da carta
     * @param card a carta que está sendo jogada
     */
    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        int dano = card.getAttack();
        target.setHealthPoints(target.getHealthPoints() - dano);
    }
}
