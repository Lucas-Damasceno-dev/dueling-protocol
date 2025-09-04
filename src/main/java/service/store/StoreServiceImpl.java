package service.store;

import java.util.List;
import model.Card;
import model.CardPack;
import model.Player;

public class StoreServiceImpl implements StoreService {

    @Override
    public boolean purchaseCardPack(Player player, String packType) {
        CardPack pack = CardPackFactory.createCardPack(packType);

        synchronized (player) {
            if (player.getCoins() >= pack.getCost()) {
                player.setCoins(player.getCoins() - pack.getCost());

                List<Card> newCards = pack.open();

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