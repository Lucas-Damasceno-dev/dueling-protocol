package service.store;

import model.Player;

public interface StoreService {
    boolean purchaseCardPack(Player player, String packType);
}