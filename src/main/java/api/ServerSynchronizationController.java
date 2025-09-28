package api;

import org.springframework.beans.factory.annotation.Autowired; // <-- Import adicionado
import org.springframework.web.bind.annotation.*;
import controller.GameFacade;
import model.Player;

@RestController
@RequestMapping("/api/sync")
public class ServerSynchronizationController {

    private final GameFacade gameFacade;

    @Autowired // <-- CORREÇÃO: Anotação adicionada
    public ServerSynchronizationController(GameFacade gameFacade) {
        this.gameFacade = gameFacade;
    }

    @PostMapping("/matchmaking/enter")
    public String enterMatchmaking(@RequestBody Player player) {
        gameFacade.enterMatchmaking(player);
        return "Player " + player.getId() + " added to matchmaking queue.";
    }

    @PutMapping("/player/{id}")
    public String updatePlayer(@PathVariable String id, @RequestBody Player updatedPlayer) {
        // gameFacade.updatePlayerState(updatedPlayer);
        return "Player " + updatedPlayer.getId() + " state updated.";
    }
}