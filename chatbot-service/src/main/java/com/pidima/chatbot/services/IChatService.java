package com.pidima.chatbot.services;

import com.pidima.chatbot.models.ChatMessage;

import java.util.List;
import java.util.Optional;

/**
 * Contract for chat operations: creating sessions, fetching history, and generating replies.
 */
public interface IChatService {
    /**
     * Creates a new chat session.
     *
     * @param userId optional user id to associate with the session
     * @return the generated session id
     */
    String createSession(Optional<String> userId);

    /**
     * Returns the full message history for the provided session id.
     *
     * @param sessionId the id of the session
     * @return ordered list of chat messages
     * @throws java.util.NoSuchElementException if the session does not exist
     */
    List<ChatMessage> getHistory(String sessionId);

    /**
     * Appends a user message and returns the assistant reply.
     *
     * @param sessionId the id of the session
     * @param userMessage the user's message
     * @return the assistant reply
     * @throws java.util.NoSuchElementException if the session does not exist
     */
    String addMessageAndReply(String sessionId, String userMessage);
}
