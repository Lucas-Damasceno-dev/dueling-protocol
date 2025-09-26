package model;

/**
 * Interface que define o contrato para efeitos de cartas no jogo.
 * Todas as classes que implementam efeitos de cartas devem implementar
 * este método para definir como o efeito é aplicado durante uma partida.
 */
public interface CardEffect {
    /**
     * Executa o efeito da carta em uma sessão de jogo.
     *
     * @param session a sessão de jogo onde o efeito será aplicado
     * @param caster o jogador que está lançando a carta
     * @param target o jogador alvo do efeito da carta
     * @param card a carta que está sendo jogada
     */
    void execute(GameSession session, Player caster, Player target, Card card);
}