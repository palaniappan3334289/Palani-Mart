package com.palani.palanimart.dto;

import java.io.Serializable;

/**
 * Data Transfer Object for a single turn in a chat conversation history.
 */
public class ChatMessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String role; // "user" or "assistant" / "model"
    private String content;

    public ChatMessageDTO() {
    }

    public ChatMessageDTO(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
