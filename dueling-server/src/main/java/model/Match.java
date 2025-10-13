package model;

import java.util.UUID;

/**
 * Represents a match between two players.
 * <p>
 * <b>Failover Behavior:</b>
 * In a distributed environment, if the server instance hosting this match fails,
 * the system will detect the failure via health checks. The match will be automatically
 * terminated, and the player on the non-failing server will be declared the winner.
 * This ensures that games do not remain in an orphaned state and players are
 * properly notified.
 */
public class Match {

    public enum Status {
        WAITING_FOR_PLAYERS,
        IN_PROGRESS,
        FINISHED,
        ABORTED
    }

    private String id;
    private Player player1;
    private Player player2;
    private Status status;
    private String serverUrl;
    private Player winner;

    public Match(Player player1, Player player2) {
        this.id = UUID.randomUUID().toString();
        this.player1 = player1;
        this.player2 = player2;
        this.status = Status.WAITING_FOR_PLAYERS;
    }

    public String getId() {
        return id;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public Player getWinner() {
        return winner;
    }

    public void setWinner(Player winner) {
        this.winner = winner;
    }
}
