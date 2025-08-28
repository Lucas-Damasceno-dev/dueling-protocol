package model;

import java.util.List;
import java.util.ArrayList;

public class Player {
    private String id;
    private String nickname;
    private int coins;
    private List<Card> cardCollection;
    
    public Player() {
        this.cardCollection = new ArrayList<>();
    }
    
    public Player(String id, String nickname) {
        this();
        this.id = id;
        this.nickname = nickname;
        this.coins = 1000; // Moedas iniciais
    }
    
    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    
    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }
    
    public List<Card> getCardCollection() { return cardCollection; }
    public void setCardCollection(List<Card> cardCollection) { this.cardCollection = cardCollection; }
}
