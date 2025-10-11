package model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "player_rankings")
public class PlayerRanking implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "player_id")
    private String playerId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "player_id")
    private Player player;

    @Column(name = "elo_rating")
    private int eloRating = 1200; // Default Elo rating

    public PlayerRanking() {
    }

    public PlayerRanking(Player player) {
        this.player = player;
        this.playerId = player.getId();
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public int getEloRating() {
        return eloRating;
    }

    public void setEloRating(int eloRating) {
        this.eloRating = eloRating;
    }
}
