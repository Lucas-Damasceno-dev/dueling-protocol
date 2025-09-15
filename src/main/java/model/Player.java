package model;

import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Representa um jogador no jogo Dueling Protocol.
 * Um jogador possui atributos como vida, moedas, cartas e características
 * como raça e classe que influenciam seus atributos base.
 */
public class Player {
    private String id;
    private String nickname;
    private int coins;
    private List<Card> cardCollection;
    
    private String playerRace;
    private String playerClass;
    private int healthPoints;
    private int upgradePoints;
    private int baseAttack;
    private int baseDefense;
    private int baseMana;
    
    private static final Logger logger = LoggerFactory.getLogger(Player.class);

    /**
     * Construtor padrão para a classe Player.
     * Inicializa a coleção de cartas como uma lista vazia.
     */
    public Player() {
    this.cardCollection = new ArrayList<>();
    }
    
    /**
     * Construtor para criar um jogador com ID e nickname especificados.
     * Inicializa os atributos padrão do jogador, incluindo moedas, pontos de vida
     * e um deck inicial de cartas.
     *
     * @param id Identificador único do jogador
     * @param nickname Nome de exibição do jogador
     */
    public Player(String id, String nickname) {
    this.cardCollection = new ArrayList<>();
    this.id = id;
    this.nickname = nickname;
    this.coins = 1000;
    this.healthPoints = 100;
    this.upgradePoints = 0;
    initializeStarterDeck();
    logger.debug("Novo jogador criado: {} ({})", nickname, id);
    }

    /**
     * Configura a raça e classe do personagem do jogador.
     * Aplica os bônus de atributos correspondentes à combinação de raça e classe.
     *
     * @param race Raça do personagem (ex: "Elfo", "Anão", "Humano", "Orc")
     * @param playerClass Classe do personagem (ex: "Mago", "Arqueiro", "Guerreiro", "Ladino")
     */
    public void setCharacter(String race, String playerClass) {
        this.playerRace = race;
        this.playerClass = playerClass;
        applyAttributeBonuses();
        logger.info("Personagem configurado: {} como {} {}", id, race, playerClass);
    }

    /**
     * Inicializa o deck inicial do jogador com cartas básicas.
     * Adiciona 5 cartas básicas do tipo ATTACK ao deck do jogador.
     */
    private void initializeStarterDeck() {
        // Add basic cards to starter deck
        for (int i = 0; i < 5; i++) {
            this.cardCollection.add(new Card(
                "basic-" + i,
                "Carta Básica " + i,
                1, 1, "Comum",
                Card.CardType.ATTACK,
                "Ataque padrão", 1
            ));
        }
        logger.debug("Deck inicial criado com {} cartas para o jogador {}", cardCollection.size(), id);
    }

    /**
     * Aplica os bônus de atributos baseados na raça e classe do jogador.
     * Os bônus variam de acordo com a combinação específica de raça e classe.
     */
    private void applyAttributeBonuses() {
        // Reset attributes
        this.baseAttack = 10;
        this.baseDefense = 10;
        this.baseMana = 5;
        
        // Apply race and class bonuses
        if ("Elfo".equals(this.playerRace)) {
            this.baseMana += 3;
            if ("Mago".equals(this.playerClass)) {
                this.baseAttack += 2;
                this.baseMana += 5;
            } else if ("Arqueiro".equals(this.playerClass)) {
                this.baseAttack += 5;
                this.baseDefense += 2;
            }
        } else if ("Anão".equals(this.playerRace)) {
            this.baseDefense += 5;
            if ("Guerreiro".equals(this.playerClass)) {
                this.baseAttack += 5;
                this.baseDefense += 5;
            } else if ("Ladino".equals(this.playerClass)) {
                this.baseAttack += 3;
                this.baseDefense += 2;
                this.baseMana += 2;
            }
        } else if ("Humano".equals(this.playerRace)) {
            // Balanced bonuses
            this.baseAttack += 2;
            this.baseDefense += 2;
            this.baseMana += 2;
        } else if ("Orc".equals(this.playerRace)) {
            this.baseAttack += 5;
            this.baseDefense += 3;
            this.baseMana -= 1;
        }
        
        logger.debug("Atributos aplicados para {}: Atk={}, Def={}, Mana={}", 
                    id, baseAttack, baseDefense, baseMana);
    }

    // Getters and setters
    /**
     * Retorna a raça do personagem do jogador.
     *
     * @return a raça do personagem
     */
    public String getPlayerRace() { return playerRace; }
    
    /**
     * Define a raça do personagem do jogador.
     *
     * @param playerRace a nova raça do personagem
     */
    public void setPlayerRace(String playerRace) { this.playerRace = playerRace; }
    
    /**
     * Retorna a classe do personagem do jogador.
     *
     * @return a classe do personagem
     */
    public String getPlayerClass() { return playerClass; }
    
    /**
     * Define a classe do personagem do jogador.
     *
     * @param playerClass a nova classe do personagem
     */
    public void setPlayerClass(String playerClass) { this.playerClass = playerClass; }
    
    /**
     * Retorna os pontos de vida do jogador.
     *
     * @return os pontos de vida do jogador
     */
    public int getHealthPoints() { return healthPoints; }
    
    /**
     * Define os pontos de vida do jogador.
     * Valores negativos são convertidos para zero.
     *
     * @param healthPoints os novos pontos de vida do jogador
     */
    public void setHealthPoints(int healthPoints) { 
        this.healthPoints = Math.max(0, healthPoints); // Ensure health doesn't go below 0
    }
    
    /**
     * Retorna os pontos de melhoria disponíveis para o jogador.
     *
     * @return os pontos de melhoria do jogador
     */
    public int getUpgradePoints() { return upgradePoints; }
    
    /**
     * Define os pontos de melhoria do jogador.
     * Valores negativos são convertidos para zero.
     *
     * @param upgradePoints os novos pontos de melhoria do jogador
     */
    public void setUpgradePoints(int upgradePoints) { this.upgradePoints = Math.max(0, upgradePoints); }
    
    /**
     * Retorna o ataque base do jogador.
     *
     * @return o ataque base do jogador
     */
    public int getBaseAttack() { return baseAttack; }
    
    /**
     * Define o ataque base do jogador.
     * Valores negativos são convertidos para zero.
     *
     * @param baseAttack o novo ataque base do jogador
     */
    public void setBaseAttack(int baseAttack) { this.baseAttack = Math.max(0, baseAttack); }
    
    /**
     * Retorna a defesa base do jogador.
     *
     * @return a defesa base do jogador
     */
    public int getBaseDefense() { return baseDefense; }
    
    /**
     * Define a defesa base do jogador.
     * Valores negativos são convertidos para zero.
     *
     * @param baseDefense a nova defesa base do jogador
     */
    public void setBaseDefense(int baseDefense) { this.baseDefense = Math.max(0, baseDefense); }
    
    /**
     * Retorna o mana base do jogador.
     *
     * @return o mana base do jogador
     */
    public int getBaseMana() { return baseMana; }
    
    /**
     * Define o mana base do jogador.
     * Valores negativos são convertidos para zero.
     *
     * @param baseMana o novo mana base do jogador
     */
    public void setBaseMana(int baseMana) { this.baseMana = Math.max(0, baseMana); }

    /**
     * Retorna o identificador único do jogador.
     *
     * @return o ID do jogador
     */
    public String getId() { return id; }
    
    /**
     * Define o identificador único do jogador.
     *
     * @param id o novo ID do jogador
     */
    public void setId(String id) { this.id = id; }
    
    /**
     * Retorna o nome de exibição do jogador.
     *
     * @return o nickname do jogador
     */
    public String getNickname() { return nickname; }
    
    /**
     * Define o nome de exibição do jogador.
     *
     * @param nickname o novo nickname do jogador
     */
    public void setNickname(String nickname) { this.nickname = nickname; }
    
    /**
     * Retorna a quantidade de moedas do jogador.
     *
     * @return a quantidade de moedas do jogador
     */
    public int getCoins() { return coins; }
    
    /**
     * Define a quantidade de moedas do jogador.
     * Valores negativos são convertidos para zero.
     *
     * @param coins a nova quantidade de moedas do jogador
     */
    public void setCoins(int coins) { this.coins = Math.max(0, coins); }
    
    /**
     * Retorna a coleção de cartas do jogador.
     *
     * @return a lista de cartas do jogador
     */
    public List<Card> getCardCollection() { return cardCollection; }
    
    /**
     * Define a coleção de cartas do jogador.
     *
     * @param cardCollection a nova lista de cartas do jogador
     */
    public void setCardCollection(List<Card> cardCollection) { this.cardCollection = cardCollection; }
}