package service.store;

import model.Player;
import service.store.PurchaseResult;

public interface StoreService {
    PurchaseResult purchaseCardPack(Player player, String packType);
}
