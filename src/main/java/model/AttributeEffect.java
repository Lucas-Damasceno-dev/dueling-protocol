package model;

/**
 * Implementação do efeito de atributo para cartas do tipo ATTRIBUTE.
 * Este efeito aumenta os atributos base do jogador que lança a carta,
 * incluindo ataque, defesa e mana.
 */
public class AttributeEffect implements CardEffect {
    /**
     * {@inheritDoc}
     * Aumenta os atributos base do jogador que lança a carta:
     * - Ataque base é aumentado pelo valor de ataque da carta
     * - Defesa base é aumentada pelo valor de defesa da carta
     * - Mana base é aumentada pela metade do custo de mana da carta
     *
     * @param session a sessão de jogo onde o efeito será aplicado
     * @param caster o jogador que está lançando a carta
     * @param target o jogador alvo do efeito da carta (não utilizado neste efeito)
     * @param card a carta que está sendo jogada
     */
    @Override
    public void execute(GameSession session, Player caster, Player target, Card card) {
        // Apply attribute bonuses based on card properties
        int attackBonus = card.getAttack();
        int defenseBonus = card.getDefense();
        int manaBonus = card.getManaCost() / 2; // Mana bonus is half the mana cost
        
        caster.setBaseAttack(caster.getBaseAttack() + attackBonus);
        caster.setBaseDefense(caster.getBaseDefense() + defenseBonus);
        caster.setBaseMana(caster.getBaseMana() + manaBonus);
        
        System.out.println(caster.getNickname() + " usou '" + card.getName() + 
            "', ganhando +" + attackBonus + " ataque, +" + defenseBonus + 
            " defesa e +" + manaBonus + " mana!");
    }
}