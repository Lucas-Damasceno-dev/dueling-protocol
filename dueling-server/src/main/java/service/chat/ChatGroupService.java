package service.chat;

import model.ChatGroup;
import model.Player;
import org.springframework.stereotype.Service;
import pubsub.IEventManager;
import repository.ChatGroupRepository;
import repository.PlayerRepository;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class ChatGroupService {

    private final ChatGroupRepository chatGroupRepository;
    private final PlayerRepository playerRepository;
    private final IEventManager eventManager;

    public ChatGroupService(ChatGroupRepository chatGroupRepository, PlayerRepository playerRepository, IEventManager eventManager) {
        this.chatGroupRepository = chatGroupRepository;
        this.playerRepository = playerRepository;
        this.eventManager = eventManager;
    }

    public ChatGroup createGroup(String name, String creatorId) {
        Player creator = playerRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setName(name);
        Set<Player> members = new HashSet<>();
        members.add(creator);
        chatGroup.setMembers(members);

        return chatGroupRepository.save(chatGroup);
    }

    public ChatGroup addMember(String groupName, String playerId) {
        ChatGroup chatGroup = chatGroupRepository.findByName(groupName)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        chatGroup.getMembers().add(player);
        return chatGroupRepository.save(chatGroup);
    }

    public ChatGroup removeMember(String groupName, String playerId) {
        ChatGroup chatGroup = chatGroupRepository.findByName(groupName)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        chatGroup.getMembers().remove(player);
        return chatGroupRepository.save(chatGroup);
    }

    public Optional<ChatGroup> findGroupByName(String name) {
        return chatGroupRepository.findByName(name);
    }

    public void sendMessage(String groupName, String senderId, String content) {
        chatGroupRepository.findByName(groupName)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        eventManager.sendGroupMessage(groupName, senderId, content);
    }
}
