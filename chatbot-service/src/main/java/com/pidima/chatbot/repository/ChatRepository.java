package com.pidima.chatbot.repository;

import com.pidima.chatbot.models.ChatMessage;
import java.util.List;

/**
 * Persistence abstraction for chat message history, keyed by session id.
 */
public interface ChatRepository {
    /**
     * Creates a new session. Implementations may be a no-op when existence is inferred from messages.
     *
     * @param sessionId unique session identifier
     */
    void createSession(String sessionId);

    /**
     * Checks whether the given session exists.
     *
     * @param sessionId session id
     * @return true if session exists; false otherwise
     */
    boolean sessionExists(String sessionId);

    /**
     * Appends a message to the session history.
     *
     * @param sessionId session id
     * @param message message to append
     * @throws java.util.NoSuchElementException if the session does not exist
     */
    void appendMessage(String sessionId, ChatMessage message);

    /**
     * Returns the ordered history for the session.
     *
     * @param sessionId session id
     * @return list of messages in chronological order
     * @throws java.util.NoSuchElementException if the session does not exist
     */
    List<ChatMessage> getHistory(String sessionId);
}
