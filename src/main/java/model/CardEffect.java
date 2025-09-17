package model;

/**
 * Interface that defines the contract for card effects in the game.
 * All classes that implement card effects must implement
 * this method to define how the effect is applied during a match.
 */
public interface CardEffect {
    /**
     * Executes the card effect in a game session.
     *
     * @param session the game session where the effect will be applied
     * @param caster the player casting the card
     * @param target the target player of the card's effect
     * @param card the card being played
     */
    void execute(GameSession session, Player caster, Player target, Card card);
}