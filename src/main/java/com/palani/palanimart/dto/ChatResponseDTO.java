package com.palani.palanimart.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for chat assistant responses.
 * Provides rich replies containing text (markdown), clickable product recommendations,
 * and contextual prompt suggestions.
 */
public class ChatResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String reply;
    private List<ProductDTO> products;
    private List<String> suggestions;

    public ChatResponseDTO() {
        this.products = new ArrayList<>();
        this.suggestions = new ArrayList<>();
    }

    public ChatResponseDTO(String reply) {
        this.reply = reply;
        this.products = new ArrayList<>();
        this.suggestions = new ArrayList<>();
    }

    public ChatResponseDTO(String reply, List<ProductDTO> products, List<String> suggestions) {
        this.reply = reply;
        this.products = products != null ? products : new ArrayList<>();
        this.suggestions = suggestions != null ? suggestions : new ArrayList<>();
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public List<ProductDTO> getProducts() {
        return products;
    }

    public void setProducts(List<ProductDTO> products) {
        this.products = products;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }
}
