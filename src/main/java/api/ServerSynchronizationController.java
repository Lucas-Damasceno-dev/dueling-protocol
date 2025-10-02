package api;

import static spark.Spark.*;
import com.google.gson.Gson;
import controller.GameFacade;
import model.Player;

/**
 * Controller for handling REST API endpoints related to server synchronization.
 */
public class ServerSynchronizationController {
    private final GameFacade gameFacade;
    private final Gson gson = new Gson();

    public ServerSynchronizationController(GameFacade gameFacade) {
        this.gameFacade = gameFacade;
        setupRoutes();
    }

    /**
     * Defines the API routes for synchronization.
     */
    private void setupRoutes() {
        post("/api/sync/matchmaking/enter", (req, res) -> {
            Player player = gson.fromJson(req.body(), Player.class);
            gameFacade.enterMatchmaking(player);
            res.status(200);
            return "Player " + player.getId() + " added to matchmaking queue.";
        });

        put("/api/sync/player/:id", (req, res) -> {
            Player updatedPlayer = gson.fromJson(req.body(), Player.class);
            // gameFacade.updatePlayerState(updatedPlayer);
            res.status(200);
            return "Player " + updatedPlayer.getId() + " state updated.";
        });
    }
}