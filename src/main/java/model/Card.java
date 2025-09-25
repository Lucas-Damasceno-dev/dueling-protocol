package model;

import java.util.Objects;

public class Card {
    private String id;
    private String name;
    private int attack;
    private int defense;
    private String rarity;
    private CardType cardType;
    private String effectDescription;
    private int manaCost;

    public enum CardType {
        ATTACK,
        DEFENSE,
        MAGIC,
        ATTRIBUTE,
        SCENARIO,
        EQUIPMENT
    }

    public Card() {}

    public Card(String id, String name, int attack, int defense, String rarity, CardType cardType, String effectDescription, int manaCost) {
        this.id = Objects.requireNonNull(id, "ID não pode ser nulo");
        this.name = Objects.requireNonNull(name, "Nome não pode ser nulo");
        this.attack = Math.max(0, attack);
        this.defense = Math.max(0, defense);
        this.rarity = Objects.requireNonNull(rarity, "Raridade não pode ser nula");
        this.cardType = Objects.requireNonNull(cardType, "Tipo de carta não pode ser nulo");
        this.effectDescription = Objects.requireNonNull(effectDescription, "Descrição do efeito não pode ser nula");
        this.manaCost = Math.max(0, manaCost);
    }

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = Objects.requireNonNull(id, "ID não pode ser nulo"); }

    public String getName() { return name; }
    public void setName(String name) { this.name = Objects.requireNonNull(name, "Nome não pode ser nulo"); }

    public int getAttack() { return attack; }
    public void setAttack(int attack) { this.attack = Math.max(0, attack); }

    public int getDefense() { return defense; }
    public void setDefense(int defense) { this.defense = Math.max(0, defense); }

    public String getRarity() { return rarity; }
    public void setRarity(String rarity) { this.rarity = Objects.requireNonNull(rarity, "Raridade não pode ser nula"); }

    public CardType getCardType() { return cardType; }
    public void setCardType(CardType cardType) { this.cardType = Objects.requireNonNull(cardType, "Tipo de carta não pode ser nulo"); }

    public String getEffectDescription() { return effectDescription; }
    public void setEffectDescription(String effectDescription) { this.effectDescription = Objects.requireNonNull(effectDescription, "Descrição do efeito não pode ser nula"); }

    public int getManaCost() { return manaCost; }
    public void setManaCost(int manaCost) { this.manaCost = Math.max(0, manaCost); }

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
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", attack=" + attack +
                ", defense=" + defense +
                ", rarity='" + rarity + '\'' +
                ", cardType=" + cardType +
                ", manaCost=" + manaCost +
                '}';
    }
}