package model;

import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the attack effect for ATTACK-type cards.
 * This effect deals damage to the target player equal to the card's attack value.
 */
public class AttackEffect implements CardEffect, Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(AttackEffect.class);
    
    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        int bonus = session.consumeNextAttackBonus(caster.getId());
        int rawDamage = card.getAttack() + bonus;
        int damage = Math.max(0, rawDamage - target.getBaseDefense());
        int previousHP = target.getHealthPoints();
        target.setHealthPoints(previousHP - damage);
        
        logger.info("{} used '{}' (attack={}, bonus={}, targetDefense={}) dealing {} damage to {}", 
                caster.getNickname(), card.getName(), card.getAttack(), bonus, target.getBaseDefense(), damage, target.getNickname());
        
        session.getGameFacade().notifyPlayers(
                java.util.Arrays.asList(caster.getId(), target.getId()),
                String.format("INFO:⚔️ %s atacou com '%s' causando %d de dano! (%d → %d HP)", 
                        caster.getNickname(), card.getName(), damage, previousHP, target.getHealthPoints()));
    }
}
