package com.pidima.chatbot.api;

import com.pidima.chatbot.models.ChatMessage;
import com.pidima.chatbot.models.dto.*;
import com.pidima.chatbot.services.IChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * REST endpoints for managing chat sessions and exchanging messages with the assistant.
 * <p>
 * Base path: {@code /chat}
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class ChatController {

    private final IChatService chatService;

    /**
     * Creates a new chat session.
     *
     * @param request optional body with {@code userId} to associate the session with a user
     * @return the created session id
     */
    @PostMapping("/session")
    public ResponseEntity<CreateSessionResponse> createSession(@Valid @RequestBody(required = false) CreateSessionRequest request) {
        Optional<String> userId = request == null ? Optional.empty() : Optional.ofNullable(request.getUserId());
        String sessionId = chatService.createSession(userId);
        return ResponseEntity.ok(new CreateSessionResponse(sessionId));
    }

    /**
     * Appends a user message to the session history and returns the assistant's reply.
     *
     * @param request the chat message request containing {@code sessionId} and {@code message}
     * @return the assistant reply for the provided message
     * @throws NoSuchElementException if the session does not exist
     */
    @PostMapping("/message")
    public ResponseEntity<ChatMessageResponse> sendMessage(@Valid @RequestBody ChatMessageRequest request) {
        String reply = chatService.addMessageAndReply(request.getSessionId(), request.getMessage());
        return ResponseEntity.ok(new ChatMessageResponse(request.getSessionId(), reply));
    }

    /**
     * Returns the full chat history for the given session id.
     *
     * @param sessionId the id of the session
     * @return ordered list of chat messages (oldest first)
     * @throws NoSuchElementException if the session does not exist
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatMessage>> history(@PathVariable("sessionId") String sessionId) {
        List<ChatMessage> history = chatService.getHistory(sessionId);
        return ResponseEntity.ok(history);
    }
}
