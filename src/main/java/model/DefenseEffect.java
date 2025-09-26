package model;

/**
 * Implementação do efeito de defesa para cartas do tipo DEFENSE.
 * Este efeito aumenta a defesa base do jogador que lança a carta.
 */
public class DefenseEffect implements CardEffect {
    /**
     * {@inheritDoc}
     * Aumenta a defesa base do jogador que lança a carta pelo valor de defesa da carta.
     *
     * @param session a sessão de jogo onde o efeito será aplicado
     * @param caster o jogador que está lançando a carta
     * @param target o jogador alvo do efeito da carta (não utilizado neste efeito)
     * @param card a carta que está sendo jogada
     */
    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        int defenseBonus = card.getDefense();
        caster.setBaseDefense(caster.getBaseDefense() + defenseBonus);
        System.out.println(caster.getNickname() + " usou '" + card.getName() + "', ganhando +" + defenseBonus + " de defesa base!");
    }
}