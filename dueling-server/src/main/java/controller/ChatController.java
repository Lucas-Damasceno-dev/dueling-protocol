package controller;

import controller.dto.chat.AddMemberRequest;
import controller.dto.chat.CreateGroupRequest;
import controller.dto.chat.SendMessageRequest;
import model.ChatGroup;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.chat.ChatGroupService;

@RestController
@RequestMapping("/api/chat/groups")
public class ChatController {

    private final ChatGroupService chatGroupService;

    public ChatController(ChatGroupService chatGroupService) {
        this.chatGroupService = chatGroupService;
    }

    @PostMapping
    public ResponseEntity<ChatGroup> createGroup(@RequestBody CreateGroupRequest request) {
        ChatGroup group = chatGroupService.createGroup(request.getName(), request.getCreatorId());
        return ResponseEntity.ok(group);
    }

    @PostMapping("/{groupName}/members")
    public ResponseEntity<ChatGroup> addMember(@PathVariable String groupName, @RequestBody AddMemberRequest request) {
        ChatGroup group = chatGroupService.addMember(groupName, request.getPlayerId());
        return ResponseEntity.ok(group);
    }

    @DeleteMapping("/{groupName}/members/{playerId}")
    public ResponseEntity<ChatGroup> removeMember(@PathVariable String groupName, @PathVariable String playerId) {
        ChatGroup group = chatGroupService.removeMember(groupName, playerId);
        return ResponseEntity.ok(group);
    }

    @PostMapping("/{groupName}/messages")
    public ResponseEntity<Void> sendMessage(@PathVariable String groupName, @RequestBody SendMessageRequest request) {
        chatGroupService.sendMessage(groupName, request.getSenderId(), request.getContent());
        return ResponseEntity.ok().build();
    }
}
