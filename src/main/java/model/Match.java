package model;

import java.util.UUID;

/**
 * Representa uma partida entre dois jogadores no jogo Dueling Protocol.
 * Cada partida possui um identificador único e os dois jogadores participantes.
 */
public class Match {
    private String matchId;
    private Player player1;
    private Player player2;
    
    /**
     * Construtor para criar uma nova partida entre dois jogadores.
     * Gera automaticamente um identificador único para a partida.
     *
     * @param player1 o primeiro jogador da partida
     * @param player2 o segundo jogador da partida
     */
    public Match(Player player1, Player player2) {
        this.matchId = UUID.randomUUID().toString();
        this.player1 = player1;
        this.player2 = player2;
    }
    
    // Getters
    /**
     * Retorna o identificador único da partida.
     *
     * @return o ID da partida
     */
    public String getMatchId() { return matchId; }
    
    /**
     * Retorna o primeiro jogador da partida.
     *
     * @return o primeiro jogador
     */
    public Player getPlayer1() { return player1; }
    
    /**
     * Retorna o segundo jogador da partida.
     *
     * @return o segundo jogador
     */
    public Player getPlayer2() { return player2; }
}