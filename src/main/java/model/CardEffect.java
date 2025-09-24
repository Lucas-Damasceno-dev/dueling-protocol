package model;

public interface CardEffect {
    void execute(GameSession session, Player caster, Player target, Card card);
}