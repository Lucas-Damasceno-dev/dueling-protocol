package service.emote;

import model.Emote;
import org.springframework.stereotype.Service;
import pubsub.IEventManager;
import repository.EmoteRepository;

@Service
public class EmoteService {

    private final EmoteRepository emoteRepository;
    private final IEventManager eventManager;

    public EmoteService(EmoteRepository emoteRepository, IEventManager eventManager) {
        this.emoteRepository = emoteRepository;
        this.eventManager = eventManager;
    }

    public void handleSendEmote(String senderId, String channelType, String channelId, String emoteId) {
        emoteRepository.findById(emoteId).ifPresent(emote -> {
            eventManager.sendEmote(channelType, channelId, senderId, emoteId);
        });
    }
}
