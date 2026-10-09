package com.palani.palanimart.controller;

import com.palani.palanimart.dto.ChatRequestDTO;
import com.palani.palanimart.dto.ChatResponseDTO;
import com.palani.palanimart.dto.UserResponseDTO;
import com.palani.palanimart.exception.ValidationException;
import com.palani.palanimart.service.ChatService;
import com.palani.palanimart.service.impl.ChatServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller/Servlet handling customer support AI chatbot interactions.
 * REST endpoint: POST /api/chat, /api/v1/chat
 * Features:
 * - JSON and form body support
 * - In-memory IP/Session rate limiting
 * - Rich response envelope (reply text, product recommendations, suggested chips)
 * - Safe error handling and input validation
 */
@WebServlet(name = "ChatServlet", urlPatterns = {"/api/chat", "/api/v1/chat"})
public class ChatServlet extends BaseServlet {

    private static final Logger logger = LoggerFactory.getLogger(ChatServlet.class);
    private static final int MAX_REQUESTS_PER_MINUTE = 30;

    // IP-based sliding window rate limiter
    private static final Map<String, RequestBucket> rateLimitMap = new ConcurrentHashMap<>();

    private final ChatService chatService;

    public ChatServlet() {
        this(new ChatServiceImpl());
    }

    public ChatServlet(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            // Apply rate limit
            String clientIp = getClientIp(req);
            if (isRateLimited(clientIp)) {
                writeJsonError(resp, 429, "TOO_MANY_REQUESTS", "Too many messages sent. Please wait a moment before sending another inquiry.");
                return;
            }

            ChatRequestDTO requestDto = null;

            // 1. Try reading JSON body
            try {
                requestDto = readJsonBody(req, ChatRequestDTO.class);
            } catch (Exception ignored) {
            }

            // 2. Fallback to form parameter
            if (requestDto == null || requestDto.getMessage() == null || requestDto.getMessage().trim().isEmpty()) {
                String formMessage = req.getParameter("message");
                if (formMessage != null && !formMessage.trim().isEmpty()) {
                    requestDto = new ChatRequestDTO(formMessage.trim());
                }
            }

            if (requestDto == null || requestDto.getMessage() == null || requestDto.getMessage().trim().isEmpty()) {
                throw new ValidationException("Chat message cannot be empty", "EMPTY_MESSAGE");
            }

            if (requestDto.getMessage().length() > 1000) {
                throw new ValidationException("Message exceeds 1000 characters limit", "MESSAGE_TOO_LONG");
            }

            UserResponseDTO currentUser = getSessionUser(req);
            Long userId = (currentUser != null) ? currentUser.getId() : null;

            ChatResponseDTO chatResponse = null;
            try {
                chatResponse = chatService.chat(requestDto, userId);
            } catch (Exception e) {
                logger.debug("chatService.chat threw exception, attempting processMessage: {}", e.getMessage());
            }

            // Backward compatibility fallback for mocked tests or legacy delegates
            if (chatResponse == null || chatResponse.getReply() == null) {
                String reply = chatService.processMessage(requestDto.getMessage().trim(), userId);
                chatResponse = new ChatResponseDTO(reply, Collections.emptyList(), Collections.emptyList());
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("reply", chatResponse.getReply());
            data.put("products", chatResponse.getProducts() != null ? chatResponse.getProducts() : Collections.emptyList());
            data.put("suggestions", chatResponse.getSuggestions() != null ? chatResponse.getSuggestions() : Collections.emptyList());

            writeJsonResponse(resp, HttpServletResponse.SC_OK, "Chat response generated successfully", data);
        } catch (Exception e) {
            handleException(resp, e, true);
        }
    }

    private String getClientIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        if (xf != null && !xf.trim().isEmpty()) {
            return xf.split(",")[0].trim();
        }
        return req.getRemoteAddr() != null ? req.getRemoteAddr() : "unknown";
    }

    private boolean isRateLimited(String clientIp) {
        long now = System.currentTimeMillis();
        RequestBucket bucket = rateLimitMap.computeIfAbsent(clientIp, k -> new RequestBucket(now));
        synchronized (bucket) {
            if (now - bucket.windowStart > 60000) {
                bucket.windowStart = now;
                bucket.count = 0;
            }
            bucket.count++;
            return bucket.count > MAX_REQUESTS_PER_MINUTE;
        }
    }

    private static class RequestBucket {
        long windowStart;
        int count;

        RequestBucket(long windowStart) {
            this.windowStart = windowStart;
            this.count = 0;
        }
    }
}
