package model;

import java.util.UUID;

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
