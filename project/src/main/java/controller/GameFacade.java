package controller;

import service.matchmaking.MatchmakingService;
import service.matchmaking.ConcurrentMatchmakingService;
import service.store.StoreService;
import service.store.StoreServiceImpl;
import repository.InMemoryPlayerRepository;
import model.Player;

public class GameFacade {
    private final MatchmakingService matchmakingService;
    private final StoreService storeService;

    public GameFacade() {
        this.matchmakingService = ConcurrentMatchmakingService.getInstance();
        this.storeService = new StoreServiceImpl();
    }

    public void enterMatchmaking(Player player) {
        matchmakingService.addPlayerToQueue(player);
    }

    public void tryToCreateMatch() {
        matchmakingService.findMatch().ifPresent(match -> {
        });
    }

    public void buyPack(Player player, String packType) {
        storeService.purchaseCardPack(player, packType);
    }
}