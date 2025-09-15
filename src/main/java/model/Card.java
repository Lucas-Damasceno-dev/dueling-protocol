package model;

import java.util.Objects;

/**
 * Representa uma carta no jogo Dueling Protocol.
 * Cada carta possui atributos como ataque, defesa, raridade, tipo e custo de mana.
 * As cartas são usadas pelos jogadores durante as partidas para aplicar efeitos
 * e estratégias.
 */
public class Card {
    private String id;
    private String name;
    private int attack;
    private int defense;
    private String rarity;
    private CardType cardType;
    private String effectDescription;
    private int manaCost;

    /**
     * Enumeração que define os tipos possíveis de cartas no jogo.
     */
    public enum CardType {
        /** Carta de ataque que causa dano ao oponente */
        ATTACK,
        /** Carta de defesa que protege o jogador */
        DEFENSE,
        /** Carta mágica com efeitos especiais */
        MAGIC,
        /** Carta que melhora os atributos do jogador */
        ATTRIBUTE,
        /** Carta que modifica o cenário da partida */
        SCENARIO,
        /** Carta de equipamento que fornece bônus permanentes */
        EQUIPMENT
    }

    /**
     * Construtor padrão para a classe Card.
     */
    public Card() {}

    /**
     * Construtor completo para criar uma carta com todos os atributos especificados.
     *
     * @param id Identificador único da carta
     * @param name Nome da carta
     * @param attack Valor de ataque da carta
     * @param defense Valor de defesa da carta
     * @param rarity Raridade da carta (ex: "Comum", "Rara", "Lendária")
     * @param cardType Tipo da carta, definido pela enumeração CardType
     * @param effectDescription Descrição textual do efeito da carta
     * @param manaCost Custo em mana para jogar a carta
     * @throws NullPointerException se id, name, rarity, cardType ou effectDescription forem nulos
     */
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
    /**
     * Retorna o identificador único da carta.
     *
     * @return o ID da carta
     */
    public String getId() { return id; }
    
    /**
     * Define o identificador único da carta.
     *
     * @param id o novo ID da carta
     * @throws NullPointerException se o id for nulo
     */
    public void setId(String id) { this.id = Objects.requireNonNull(id, "ID não pode ser nulo"); }

    /**
     * Retorna o nome da carta.
     *
     * @return o nome da carta
     */
    public String getName() { return name; }
    
    /**
     * Define o nome da carta.
     *
     * @param name o novo nome da carta
     * @throws NullPointerException se o nome for nulo
     */
    public void setName(String name) { this.name = Objects.requireNonNull(name, "Nome não pode ser nulo"); }

    /**
     * Retorna o valor de ataque da carta.
     *
     * @return o valor de ataque da carta
     */
    public int getAttack() { return attack; }
    
    /**
     * Define o valor de ataque da carta.
     * Valores negativos são convertidos para zero.
     *
     * @param attack o novo valor de ataque da carta
     */
    public void setAttack(int attack) { this.attack = Math.max(0, attack); }

    /**
     * Retorna o valor de defesa da carta.
     *
     * @return o valor de defesa da carta
     */
    public int getDefense() { return defense; }
    
    /**
     * Define o valor de defesa da carta.
     * Valores negativos são convertidos para zero.
     *
     * @param defense o novo valor de defesa da carta
     */
    public void setDefense(int defense) { this.defense = Math.max(0, defense); }

    /**
     * Retorna a raridade da carta.
     *
     * @return a raridade da carta
     */
    public String getRarity() { return rarity; }
    
    /**
     * Define a raridade da carta.
     *
     * @param rarity a nova raridade da carta
     * @throws NullPointerException se a raridade for nula
     */
    public void setRarity(String rarity) { this.rarity = Objects.requireNonNull(rarity, "Raridade não pode ser nula"); }

    /**
     * Retorna o tipo da carta.
     *
     * @return o tipo da carta
     */
    public CardType getCardType() { return cardType; }
    
    /**
     * Define o tipo da carta.
     *
     * @param cardType o novo tipo da carta
     * @throws NullPointerException se o tipo for nulo
     */
    public void setCardType(CardType cardType) { this.cardType = Objects.requireNonNull(cardType, "Tipo de carta não pode ser nulo"); }

    /**
     * Retorna a descrição do efeito da carta.
     *
     * @return a descrição do efeito da carta
     */
    public String getEffectDescription() { return effectDescription; }
    
    /**
     * Define a descrição do efeito da carta.
     *
     * @param effectDescription a nova descrição do efeito da carta
     * @throws NullPointerException se a descrição for nula
     */
    public void setEffectDescription(String effectDescription) { this.effectDescription = Objects.requireNonNull(effectDescription, "Descrição do efeito não pode ser nula"); }

    /**
     * Retorna o custo em mana da carta.
     *
     * @return o custo em mana da carta
     */
    public int getManaCost() { return manaCost; }
    
    /**
     * Define o custo em mana da carta.
     * Valores negativos são convertidos para zero.
     *
     * @param manaCost o novo custo em mana da carta
     */
    public void setManaCost(int manaCost) { this.manaCost = Math.max(0, manaCost); }

    /**
     * Compara esta carta com outro objeto para verificar igualdade.
     * Duas cartas são consideradas iguais se possuírem o mesmo ID.
     *
     * @param o o objeto a ser comparado
     * @return true se os objetos forem iguais, false caso contrário
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return Objects.equals(id, card.id);
    }

    /**
     * Retorna o código hash para esta carta.
     * O código hash é baseado no ID da carta.
     *
     * @return o código hash da carta
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Retorna uma representação em string desta carta.
     *
     * @return uma string que representa a carta
     */
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