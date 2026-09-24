package com.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper = new ObjectMapper();

    // group -> sessions
    private final ConcurrentMap<String, Set<WebSocketSession>> groups = new ConcurrentHashMap<>();
    // sessionId -> set of groups this session joined
    private final ConcurrentMap<String, Set<String>> sessionToGroups = new ConcurrentHashMap<>();
    // sessionId -> username
    private final ConcurrentMap<String, String> sessionToUser = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("Connected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage text) throws IOException {
        ChatMessage msg = this.mapper.readValue(text.getPayload(), ChatMessage.class);
        String type = Optional.ofNullable(msg.getType()).orElse("");

        switch (type) {
            case "join":
                this.handleJoin(session, msg);
                break;
            case "leave":
                this.handleLeave(session, msg);
                break;
            case "msg":
                this.handleMsg(session, msg);
                break;
            default:
                this.sendError(session, "unknown message type");

        }

    }

    private void sendError(WebSocketSession session, String why) throws IOException {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType("error");
        chatMessage.setContent(why);
        session.sendMessage(new TextMessage(this.mapper.writeValueAsString(chatMessage)));
    }

    private void handleMsg(WebSocketSession session, ChatMessage msg) throws IOException {
        String group = msg.getGroup();
        if (group == null || group.isBlank()) {
            this.sendError(session, "group required for message");
            return;
        }

        Set<String> joinedGroups = this.sessionToGroups.getOrDefault(session.getId(), Collections.emptySet());
        if (!joinedGroups.contains(group)) {
            this.sendError(session, "you are not a member of group: " + group);
            return;
        }
        String user = this.sessionToUser.getOrDefault(session.getId(), msg.getFrom());
        msg.setFrom(user);
        this.broadcastToGroup(group, msg);
    }

    private void handleLeave(WebSocketSession session, ChatMessage msg) throws IOException {
        String group = msg.getGroup();
        if (group == null || group.isBlank()) {
            this.sendError(session, "group required for leave");
            return;
        }
        this.removeFromGroup(group, session);
    }

    private void removeFromGroup(String group, WebSocketSession session) {
        Set<WebSocketSession> webSocketSessions = this.groups.get(group);

        if (webSocketSessions != null) {
            webSocketSessions.remove(session);
            if (webSocketSessions.isEmpty()) {
                this.groups.remove(group);
            }
        }

        Set<String> groups = this.sessionToGroups.get(session.getId());
        if (groups != null) {
            groups.remove(group);
            if (groups.isEmpty()) {
                this.sessionToGroups.remove(session.getId());
            }
        }

        String user = this.sessionToUser.getOrDefault(session.getId(), "anon");
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType("sys");
        chatMessage.setGroup(group);
        chatMessage.setFrom(user);
        chatMessage.setContent(user + " left " + group);

        try {
            this.broadcastToGroup(group, chatMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleJoin(WebSocketSession session, ChatMessage msg) throws IOException {
        String group = msg.getGroup();
        if (group == null || group.isBlank()) {
            this.sendError(session, "Group required for join");
            return;
        }

        String user = Optional.ofNullable(msg.getFrom())
                .filter(s -> !s.isBlank())
                .orElse("anon-" + session.getId().substring(0, 6));
        this.sessionToUser.put(session.getId(), user);

        this.groups.computeIfAbsent(group, k -> ConcurrentHashMap.newKeySet())
                .add(session);
        this.sessionToGroups.computeIfAbsent(session.getId(), k -> ConcurrentHashMap.newKeySet())
                .add(group);

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType("sys");
        chatMessage.setGroup(group);
        chatMessage.setFrom(user);
        chatMessage.setContent(user + " joined " + group);
        this.broadcastToGroup(group, chatMessage);

    }

    private void broadcastToGroup(String group, ChatMessage chatMessage) throws JsonProcessingException {
        Set<WebSocketSession> webSocketSessions = this.groups.get(group);
        if (webSocketSessions == null) {
            return;
        }
        String payload = this.mapper.writeValueAsString(chatMessage);
        for (WebSocketSession session : webSocketSessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(payload));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
