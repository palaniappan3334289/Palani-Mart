package com.palani.palanimart.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for incoming chat inquiries.
 * Supports single message queries as well as multi-turn conversation history.
 */
public class ChatRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String message;
    private List<ChatMessageDTO> history;

    public ChatRequestDTO() {
        this.history = new ArrayList<>();
    }

    public ChatRequestDTO(String message) {
        this.message = message;
        this.history = new ArrayList<>();
    }

    public ChatRequestDTO(String message, List<ChatMessageDTO> history) {
        this.message = message;
        this.history = history != null ? history : new ArrayList<>();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ChatMessageDTO> getHistory() {
        return history;
    }

    public void setHistory(List<ChatMessageDTO> history) {
        this.history = history;
    }
}
