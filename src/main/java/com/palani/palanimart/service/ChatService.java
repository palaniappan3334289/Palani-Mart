package com.palani.palanimart.service;

import com.palani.palanimart.dto.ChatRequestDTO;
import com.palani.palanimart.dto.ChatResponseDTO;
import com.palani.palanimart.exception.AppException;

/**
 * Service interface for AI chatbot support and customer assistance.
 * Clean abstraction keeping server-side provider and API keys isolated.
 */
public interface ChatService {

    /**
     * Processes customer inquiry and returns an intelligent contextual response string.
     * Preserved for backward compatibility with existing tests.
     *
     * @param userMessage Message query from buyer
     * @param userId      Optional authenticated user ID
     * @return AI assistant reply text
     * @throws AppException if query cannot be processed
     */
    String processMessage(String userMessage, Long userId) throws AppException;

    /**
     * Processes a full chat request containing message and history, returning
     * conversational text, structured product cards, and follow-up suggestion chips.
     *
     * @param request ChatRequestDTO with user message and conversation history
     * @param userId  Optional authenticated user ID
     * @return ChatResponseDTO with reply, products, and suggestions
     * @throws AppException on processing failure
     */
    ChatResponseDTO chat(ChatRequestDTO request, Long userId) throws AppException;
}
