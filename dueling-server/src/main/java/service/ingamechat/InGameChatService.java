package service.ingamechat;

import model.GameSession;
import org.springframework.stereotype.Service;
import pubsub.IEventManager;
import repository.GameSessionRepository;

import java.util.Optional;

@Service
public class InGameChatService {

    private final GameSessionRepository gameSessionRepository;
    private final IEventManager eventManager;

    public InGameChatService(GameSessionRepository gameSessionRepository, IEventManager eventManager) {
        this.gameSessionRepository = gameSessionRepository;
        this.eventManager = eventManager;
    }

    public void handleInGameChatMessage(String senderId, String matchId, String message) {
        Optional<GameSession> sessionOpt = gameSessionRepository.findById(matchId);
        if (sessionOpt.isPresent()) {
            GameSession session = sessionOpt.get();
            if (session.getPlayer1().getId().equals(senderId) || session.getPlayer2().getId().equals(senderId)) {
                eventManager.sendInGameMessage(matchId, senderId, message);
            } else {
                eventManager.publish(senderId, "ERROR:You are not in this match.");
            }
        } else {
            eventManager.publish(senderId, "ERROR:Match not found.");
        }
    }
}
