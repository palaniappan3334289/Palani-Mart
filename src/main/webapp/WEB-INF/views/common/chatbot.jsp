<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- PalaniMart AI Shopping Assistant Widget -->
<div id="palanimart-chatbot" class="palanimart-chatbot-root" data-context-path="${pageContext.request.contextPath}">

    <!-- Floating Action Button (FAB) -->
    <button type="button" id="chatbot-fab" class="chatbot-fab" aria-label="Open PalaniMart AI Shopping Assistant" aria-expanded="false" title="Chat with PalaniMart Assistant">
        <span class="chatbot-fab-icon chatbot-fab-icon--chat" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
            </svg>
        </span>
        <span class="chatbot-fab-icon chatbot-fab-icon--close" aria-hidden="true" style="display: none;">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
        </span>
        <span class="chatbot-fab-badge" aria-hidden="true">AI</span>
    </button>

    <!-- Chatbot Window -->
    <div id="chatbot-window" class="chatbot-window" role="dialog" aria-labelledby="chatbot-header-title" aria-modal="false" aria-hidden="true">
        
        <!-- Window Header -->
        <div class="chatbot-header">
            <div class="chatbot-header-brand">
                <div class="chatbot-avatar" aria-hidden="true">
                    <img src="${pageContext.request.contextPath}/assets/img/logo-mark.svg" alt="" width="24" height="24">
                </div>
                <div>
                    <h3 id="chatbot-header-title" class="chatbot-title">PalaniMart Assistant</h3>
                    <div class="chatbot-status">
                        <span class="chatbot-status-dot" aria-hidden="true"></span>
                        <span class="chatbot-status-text">AI Shopping Guide</span>
                    </div>
                </div>
            </div>
            <div class="chatbot-header-actions">
                <button type="button" id="chatbot-btn-clear" class="chatbot-header-btn" title="Clear conversation" aria-label="Clear chat history">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
                        <path d="M3 6h18"></path>
                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                    </svg>
                </button>
                <button type="button" id="chatbot-btn-minimize" class="chatbot-header-btn" title="Minimize chat" aria-label="Minimize chatbot window">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
                        <line x1="5" y1="12" x2="19" y2="12"></line>
                    </svg>
                </button>
                <button type="button" id="chatbot-btn-close" class="chatbot-header-btn" title="Close chat" aria-label="Close chatbot window">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
                        <line x1="18" y1="6" x2="6" y2="18"></line>
                        <line x1="6" y1="6" x2="18" y2="18"></line>
                    </svg>
                </button>
            </div>
        </div>

        <!-- Decorative Korvai Motif Border -->
        <div class="chatbot-korvai" aria-hidden="true"></div>

        <!-- Chat Messages Container -->
        <div id="chatbot-messages" class="chatbot-messages" role="log" aria-live="polite">
            <!-- Initial Greeting -->
            <div class="chat-msg chat-msg--assistant">
                <div class="chat-msg-avatar" aria-hidden="true">
                    <img src="${pageContext.request.contextPath}/assets/img/logo-mark.svg" alt="" width="20" height="20">
                </div>
                <div class="chat-msg-bubble">
                    <p>Hi! Welcome to Palanimar. How can I help you shop today?</p>
                </div>
            </div>

            <!-- Starter Quick Suggestion Chips -->
            <div class="chatbot-starter-chips" id="chatbot-starter-chips">
                <span class="chatbot-chip-label">Popular queries:</span>
                <div class="chatbot-chips-wrap">
                    <button type="button" class="chatbot-chip" data-prompt="Show me products under $50.">💰 Under $50</button>
                    <button type="button" class="chatbot-chip" data-prompt="Show me top electronics.">⚡ Electronics</button>
                    <button type="button" class="chatbot-chip" data-prompt="What is your return policy?">📦 Return Policy</button>
                    <button type="button" class="chatbot-chip" data-prompt="Where is my order?">🔍 Track Order</button>
                    <button type="button" class="chatbot-chip" data-prompt="Compare mechanical keyboard and headphones.">⚖️ Compare Products</button>
                </div>
            </div>
        </div>

        <!-- Typing Indicator -->
        <div id="chatbot-typing" class="chatbot-typing" style="display: none;" aria-hidden="true">
            <div class="chat-msg-avatar">
                <img src="${pageContext.request.contextPath}/assets/img/logo-mark.svg" alt="" width="20" height="20">
            </div>
            <div class="chat-msg-bubble typing-bubble">
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
            </div>
        </div>

        <!-- Error / Retry Container -->
        <div id="chatbot-error-banner" class="chatbot-error-banner" style="display: none;">
            <span id="chatbot-error-text">Unable to reach assistant.</span>
            <button type="button" id="chatbot-btn-retry" class="btn-chat-retry">Retry</button>
        </div>

        <!-- Input Bar -->
        <div class="chatbot-input-bar">
            <form id="chatbot-form" class="chatbot-form" onsubmit="return false;">
                <input type="text" id="chatbot-input" class="chatbot-input" 
                       placeholder="Ask about products, orders, returns..." 
                       maxlength="1000" 
                       autocomplete="off" 
                       aria-label="Your message for PalaniMart assistant" />
                <button type="submit" id="chatbot-btn-send" class="chatbot-btn-send" aria-label="Send message" title="Send (Enter)">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18">
                        <line x1="22" y1="2" x2="11" y2="13"></line>
                        <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
                    </svg>
                </button>
            </form>
            <div class="chatbot-footer-hint">
                <span>Press Enter to send · Grounded in real store catalog</span>
            </div>
        </div>

    </div>
</div>
