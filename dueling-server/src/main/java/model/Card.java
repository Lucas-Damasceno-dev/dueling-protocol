package model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "cards")
public class Card implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "id", unique = true, nullable = false)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "attack")
    private int attack;

    @Column(name = "defense")
    private int defense;

    @Column(name = "rarity")
    private String rarity;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false)
    private CardType cardType;

    @Column(name = "effect_description", length = 512)
    private String effectDescription;

    @Column(name = "mana_cost")
    private int manaCost;

    @ElementCollection
    @CollectionTable(name = "card_effect_parameters", joinColumns = @JoinColumn(name = "card_id"))
    @MapKeyColumn(name = "parameter_name")
    @Column(name = "parameter_value")
    private Map<String, String> effectParameters;

    @Column(name = "blockchain_token_id")
    private Long blockchainTokenId;

    public enum CardType {
        ATTACK,
        DEFENSE,
        MAGIC,
        ATTRIBUTE,
        SCENARIO,
        EQUIPMENT,
        COMBO,
        COUNTER_SPELL
    }

    public Card() {}

    public Card(String id, String name, int attack, int defense, String rarity, CardType cardType, String effectDescription, int manaCost) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.attack = Math.max(0, attack);
        this.defense = Math.max(0, defense);
        this.rarity = Objects.requireNonNull(rarity, "Rarity cannot be null");
        this.cardType = Objects.requireNonNull(cardType, "Card type cannot be null");
        this.effectDescription = Objects.requireNonNull(effectDescription, "Effect description cannot be null");
        this.manaCost = Math.max(0, manaCost);
    }

    public Card(String id, String name, int attack, int defense, String rarity, CardType cardType, String effectDescription, int manaCost, Map<String, String> effectParameters) {
        this(id, name, attack, defense, rarity, cardType, effectDescription, manaCost);
        this.effectParameters = effectParameters;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = Objects.requireNonNull(id, "ID cannot be null"); }
    public String getName() { return name; }
    public void setName(String name) { this.name = Objects.requireNonNull(name, "Name cannot be null"); }
    public int getAttack() { return attack; }
    public void setAttack(int attack) { this.attack = Math.max(0, attack); }
    public int getDefense() { return defense; }
    public void setDefense(int defense) { this.defense = Math.max(0, defense); }
    public String getRarity() { return rarity; }
    public void setRarity(String rarity) { this.rarity = Objects.requireNonNull(rarity, "Rarity cannot be null"); }
    public CardType getCardType() { return cardType; }
    public void setCardType(CardType cardType) { this.cardType = Objects.requireNonNull(cardType, "Card type cannot be null"); }
    public String getEffectDescription() { return effectDescription; }
    public void setEffectDescription(String effectDescription) { this.effectDescription = Objects.requireNonNull(effectDescription, "Effect description cannot be null"); }
    public int getManaCost() { return manaCost; }
    public void setManaCost(int manaCost) { this.manaCost = Math.max(0, manaCost); }
    public Map<String, String> getEffectParameters() {
        return effectParameters;
    }

    public void setEffectParameters(Map<String, String> effectParameters) {
        this.effectParameters = effectParameters;
    }

    public Long getBlockchainTokenId() {
        return blockchainTokenId;
    }

    public void setBlockchainTokenId(Long blockchainTokenId) {
        this.blockchainTokenId = blockchainTokenId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return Objects.equals(id, card.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Card{" +
                "id='" + id + "'" +
                ", name='" + name + "'" +
                ", attack=" + attack +
                ", defense=" + defense +
                ", rarity='" + rarity + "'" +
                ", cardType=" + cardType +
                ", manaCost=" + manaCost +
                '}';
    }
}
