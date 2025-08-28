package service.store;

import java.util.List;
import java.util.ArrayList;
import model.Card;
import model.CardPack;
import model.Player;

public class StoreServiceImpl implements StoreService {

    @Override
    public boolean purchaseCardPack(Player player, String packType) {
        CardPack pack = CardPackFactory.createCardPack(packType);

        // Bloco sincronizado no objeto do jogador para garantir a atomicidade da transação
        synchronized (player) {
            if (player.getCoins() >= pack.getCost()) {
                // 1. Debita o custo
                player.setCoins(player.getCoins() - pack.getCost());

                // 2. Abre o pacote e recebe as cartas
                List<Card> newCards = pack.open();

                // 3. Adiciona as cartas à coleção do jogador
                player.getCardCollection().addAll(newCards);

                System.out.println(player.getNickname() + " comprou um " + pack.getName());
                return true;
            } else {
                System.out.println(player.getNickname() + " não tem moedas suficientes.");
                return false;
            }
        }
    }
}