/**
 * ==========================================================================
 * PalaniMart - AI Shopping Assistant Client
 * Vanilla JavaScript: State management, CSRF integration, responsive dialog,
 * markdown parsing, real-time product card actions, and session continuity.
 * ==========================================================================
 */

(function () {
    "use strict";

    const STORAGE_KEY = "palanimart_chat_history";
    const STATE_KEY = "palanimart_chat_open";

    const Chatbot = {
        contextPath: "",
        isOpen: false,
        isMinimized: false,
        isLoading: false,
        lastFailedMessage: null,
        history: [],

        init: function () {
            const root = document.getElementById("palanimart-chatbot");
            if (!root) return;

            this.contextPath = root.dataset.contextPath || (window.PalaniMart ? window.PalaniMart.contextPath : "");

            this.fab = document.getElementById("chatbot-fab");
            this.window = document.getElementById("chatbot-window");
            this.messagesContainer = document.getElementById("chatbot-messages");
            this.form = document.getElementById("chatbot-form");
            this.input = document.getElementById("chatbot-input");
            this.btnSend = document.getElementById("chatbot-btn-send");
            this.typing = document.getElementById("chatbot-typing");
            this.errorBanner = document.getElementById("chatbot-error-banner");
            this.errorText = document.getElementById("chatbot-error-text");
            this.btnRetry = document.getElementById("chatbot-btn-retry");
            this.btnClear = document.getElementById("chatbot-btn-clear");
            this.btnMinimize = document.getElementById("chatbot-btn-minimize");
            this.btnClose = document.getElementById("chatbot-btn-close");

            this.bindEvents();
            this.restoreSession();
        },

        bindEvents: function () {
            // FAB Toggle
            if (this.fab) {
                this.fab.addEventListener("click", () => this.toggleChat());
            }

            // Window Header Controls
            if (this.btnClose) {
                this.btnClose.addEventListener("click", () => this.closeChat());
            }

            if (this.btnMinimize) {
                this.btnMinimize.addEventListener("click", () => this.toggleMinimize());
            }

            if (this.btnClear) {
                this.btnClear.addEventListener("click", () => this.clearChat());
            }

            // Form Submit (Enter or Click Send)
            if (this.form) {
                this.form.addEventListener("submit", (e) => {
                    e.preventDefault();
                    this.handleSendMessage();
                });
            }

            // Retry Button
            if (this.btnRetry) {
                this.btnRetry.addEventListener("click", () => {
                    if (this.lastFailedMessage) {
                        const msg = this.lastFailedMessage;
                        this.hideError();
                        this.sendQuery(msg);
                    }
                });
            }

            // Suggested prompt chips delegation
            if (this.messagesContainer) {
                this.messagesContainer.addEventListener("click", (e) => {
                    const chip = e.target.closest(".chatbot-chip");
                    if (chip && chip.dataset.prompt) {
                        const prompt = chip.dataset.prompt;
                        this.input.value = prompt;
                        this.handleSendMessage();
                    }
                });
            }

            // Keyboard accessibility (Escape to close)
            document.addEventListener("keydown", (e) => {
                if (e.key === "Escape" && this.isOpen && !this.isMinimized) {
                    this.closeChat();
                }
            });
        },

        toggleChat: function () {
            if (this.isOpen) {
                this.closeChat();
            } else {
                this.openChat();
            }
        },

        openChat: function () {
            this.isOpen = true;
            this.isMinimized = false;
            this.window.classList.add("is-open");
            this.window.classList.remove("is-minimized");
            this.window.setAttribute("aria-hidden", "false");
            this.fab.setAttribute("aria-expanded", "true");

            const chatIcon = this.fab.querySelector(".chatbot-fab-icon--chat");
            const closeIcon = this.fab.querySelector(".chatbot-fab-icon--close");
            if (chatIcon) chatIcon.style.display = "none";
            if (closeIcon) closeIcon.style.display = "flex";

            sessionStorage.setItem(STATE_KEY, "open");

            setTimeout(() => {
                if (this.input) this.input.focus();
                this.scrollToBottom();
            }, 100);
        },

        closeChat: function () {
            this.isOpen = false;
            this.window.classList.remove("is-open");
            this.window.setAttribute("aria-hidden", "true");
            this.fab.setAttribute("aria-expanded", "false");

            const chatIcon = this.fab.querySelector(".chatbot-fab-icon--chat");
            const closeIcon = this.fab.querySelector(".chatbot-fab-icon--close");
            if (chatIcon) chatIcon.style.display = "flex";
            if (closeIcon) closeIcon.style.display = "none";

            sessionStorage.setItem(STATE_KEY, "closed");
        },

        toggleMinimize: function () {
            this.isMinimized = !this.isMinimized;
            this.window.classList.toggle("is-minimized", this.isMinimized);
            if (!this.isMinimized) {
                this.scrollToBottom();
                if (this.input) this.input.focus();
            }
        },

        clearChat: function () {
            if (!confirm("Are you sure you want to clear your conversation history?")) {
                return;
            }
            this.history = [];
            sessionStorage.removeItem(STORAGE_KEY);

            // Reset UI with welcome message and starters
            this.messagesContainer.innerHTML = `
                <div class="chat-msg chat-msg--assistant">
                    <div class="chat-msg-avatar" aria-hidden="true">
                        <img src="${this.contextPath}/assets/img/logo-mark.svg" alt="" width="20" height="20">
                    </div>
                    <div class="chat-msg-bubble">
                        <p>Hi! Welcome to Palanimar. How can I help you shop today?</p>
                    </div>
                </div>
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
            `;
            this.hideError();
            if (this.input) this.input.focus();
        },

        handleSendMessage: function () {
            if (this.isLoading) return;
            const message = this.input.value.trim();
            if (!message) return;

            this.input.value = "";
            this.hideError();
            this.appendUserMessage(message);
            this.sendQuery(message);
        },

        sendQuery: async function (message) {
            this.setLoading(true);
            this.lastFailedMessage = null;

            const csrfToken = window.PalaniMart ? window.PalaniMart.getCsrfToken() : "";
            const headers = {
                "Content-Type": "application/json",
                "Accept": "application/json"
            };
            if (csrfToken) {
                headers["X-CSRF-Token"] = csrfToken;
            }

            const payload = {
                message: message,
                history: this.history.slice(-6)
            };

            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 15000);

            try {
                const response = await fetch(this.contextPath + "/api/chat", {
                    method: "POST",
                    headers: headers,
                    body: JSON.stringify(payload),
                    signal: controller.signal
                });

                clearTimeout(timeoutId);

                const data = await response.json();

                if (response.ok && data.success) {
                    const reply = data.data.reply || "Thank you for reaching out!";
                    const products = data.data.products || [];
                    const suggestions = data.data.suggestions || [];

                    this.appendAssistantMessage(reply, products, suggestions);

                    // Update local history
                    this.history.push({ role: "user", content: message });
                    this.history.push({ role: "assistant", content: reply });
                    this.persistHistory();
                } else if (response.status === 429) {
                    this.showError("You're sending messages too fast. Please wait a moment before trying again.", message);
                } else {
                    const errMsg = (data.error && data.error.message) ? data.error.message : "Unable to generate AI reply.";
                    this.showError(errMsg, message);
                }
            } catch (err) {
                clearTimeout(timeoutId);
                if (err.name === "AbortError") {
                    this.showError("Request timed out. Please check your connection and retry.", message);
                } else {
                    this.showError("Network error connecting to assistant.", message);
                }
            } finally {
                this.setLoading(false);
            }
        },

        appendUserMessage: function (text) {
            const msgEl = document.createElement("div");
            msgEl.className = "chat-msg chat-msg--user";
            msgEl.innerHTML = `
                <div class="chat-msg-bubble">
                    <p>${this.escapeHtml(text)}</p>
                </div>
            `;
            this.messagesContainer.appendChild(msgEl);
            this.scrollToBottom();
        },

        appendAssistantMessage: function (replyText, products, suggestions) {
            const msgEl = document.createElement("div");
            msgEl.className = "chat-msg chat-msg--assistant";

            let html = `
                <div class="chat-msg-avatar" aria-hidden="true">
                    <img src="${this.contextPath}/assets/img/logo-mark.svg" alt="" width="20" height="20">
                </div>
                <div class="chat-msg-bubble">
                    ${this.renderMarkdown(replyText)}
            `;

            // Append Product Cards if returned
            if (products && products.length > 0) {
                html += `<div class="chatbot-products-carousel">`;
                products.forEach(p => {
                    const inStock = p.stockQty > 0;
                    const badgeClass = inStock ? "chat-product-badge--in" : "chat-product-badge--out";
                    const badgeText = inStock ? "In Stock" : "Sold Out";
                    const imgHtml = p.imageUrl
                        ? `<img src="${this.escapeHtml(p.imageUrl)}" alt="${this.escapeHtml(p.name)}" loading="lazy">`
                        : `<span class="placeholder">★</span>`;

                    html += `
                        <div class="chat-product-card" data-product-id="${p.id}">
                            <div class="chat-product-media">
                                ${imgHtml}
                            </div>
                            <div class="chat-product-info">
                                <h4 class="chat-product-title">
                                    <a href="${this.contextPath}/product?id=${p.id}">${this.escapeHtml(p.name)}</a>
                                </h4>
                                <div class="chat-product-meta">
                                    <span class="chat-product-price">$${parseFloat(p.price).toFixed(2)}</span>
                                    <span class="chat-product-badge ${badgeClass}">${badgeText}</span>
                                </div>
                                <div class="chat-product-actions">
                                    <button type="button" class="btn-chat-add" ${!inStock ? "disabled" : ""} data-id="${p.id}">
                                        ${inStock ? "+ Add to Cart" : "Sold Out"}
                                    </button>
                                    <a href="${this.contextPath}/product?id=${p.id}" class="btn-chat-view">Details</a>
                                </div>
                            </div>
                        </div>
                    `;
                });
                html += `</div>`;
            }

            // Append Suggestions if returned
            if (suggestions && suggestions.length > 0) {
                html += `
                    <div class="chatbot-followup-chips">
                        <span class="chatbot-chip-label">Suggestions:</span>
                        <div class="chatbot-chips-wrap">
                `;
                suggestions.forEach(s => {
                    html += `<button type="button" class="chatbot-chip" data-prompt="${this.escapeHtml(s)}">${this.escapeHtml(s)}</button>`;
                });
                html += `</div></div>`;
            }

            html += `</div>`;
            msgEl.innerHTML = html;

            this.messagesContainer.appendChild(msgEl);

            // Bind Add to Cart buttons inside this newly appended message
            msgEl.querySelectorAll(".btn-chat-add").forEach(btn => {
                btn.addEventListener("click", (e) => {
                    const prodId = e.currentTarget.dataset.id;
                    this.handleAddToCart(prodId, btn);
                });
            });

            this.scrollToBottom();
        },

        handleAddToCart: async function (productId, button) {
            if (!productId) return;
            const originalText = button.textContent;
            button.disabled = true;
            button.textContent = "Adding...";

            const csrfToken = window.PalaniMart ? window.PalaniMart.getCsrfToken() : "";
            const headers = {
                "Content-Type": "application/json",
                "Accept": "application/json"
            };
            if (csrfToken) headers["X-CSRF-Token"] = csrfToken;

            try {
                const res = await fetch(this.contextPath + "/api/cart/add", {
                    method: "POST",
                    headers: headers,
                    body: JSON.stringify({ productId: parseInt(productId), quantity: 1 })
                });

                const data = await res.json();
                if (res.ok && data.success) {
                    if (window.Toast) {
                        window.Toast.success(data.message || "Product added to cart!");
                    }
                    if (window.PalaniMart && window.PalaniMart.refreshCartBadge) {
                        window.PalaniMart.refreshCartBadge();
                    }
                    button.textContent = "✓ Added";
                    setTimeout(() => {
                        button.disabled = false;
                        button.textContent = originalText;
                    }, 2000);
                } else if (res.status === 401) {
                    if (window.Toast) {
                        window.Toast.info("Please sign in to add products to your cart.");
                    }
                    setTimeout(() => {
                        window.location.href = this.contextPath + "/login?redirect=" + encodeURIComponent(window.location.pathname + window.location.search);
                    }, 1200);
                } else {
                    if (window.Toast) {
                        window.Toast.error(data.message || "Could not add product to cart.");
                    }
                    button.disabled = false;
                    button.textContent = originalText;
                }
            } catch (e) {
                if (window.Toast) {
                    window.Toast.error("Network error while adding to cart.");
                }
                button.disabled = false;
                button.textContent = originalText;
            }
        },

        setLoading: function (loading) {
            this.isLoading = loading;
            if (this.typing) {
                this.typing.style.display = loading ? "flex" : "none";
            }
            if (this.btnSend) {
                this.btnSend.disabled = loading;
            }
            if (loading) {
                this.scrollToBottom();
            }
        },

        showError: function (message, originalQuery) {
            this.lastFailedMessage = originalQuery;
            if (this.errorText) {
                this.errorText.textContent = message;
            }
            if (this.errorBanner) {
                this.errorBanner.style.display = "flex";
            }
            this.scrollToBottom();
        },

        hideError: function () {
            if (this.errorBanner) {
                this.errorBanner.style.display = "none";
            }
        },

        scrollToBottom: function () {
            if (this.messagesContainer) {
                this.messagesContainer.scrollTop = this.messagesContainer.scrollHeight;
            }
        },

        escapeHtml: function (text) {
            if (!text) return "";
            return String(text)
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;")
                .replace(/"/g, "&quot;")
                .replace(/'/g, "&#039;");
        },

        renderMarkdown: function (text) {
            if (!text) return "";

            let escaped = this.escapeHtml(text);

            // Markdown headers ### Title
            escaped = escaped.replace(/^### (.*$)/gim, '<h4 style="margin:6px 0 4px;font-size:0.95rem;font-weight:700;">$1</h4>');
            escaped = escaped.replace(/^## (.*$)/gim, '<h3 style="margin:8px 0 4px;font-size:1rem;font-weight:700;">$1</h3>');

            // Bold **text**
            escaped = escaped.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

            // Italic *text*
            escaped = escaped.replace(/\*(.*?)\*/g, '<em>$1</em>');

            // Inline code `code`
            escaped = escaped.replace(/`([^`]+)`/g, '<code style="background:rgba(0,0,0,0.06);padding:1px 4px;border-radius:3px;font-size:0.85em;">$1</code>');

            // Links [title](url)
            escaped = escaped.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>');

            // Bullet points
            escaped = escaped.replace(/^\s*-\s+(.*$)/gim, '<li>$1</li>');
            escaped = escaped.replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>');

            // Line breaks
            escaped = escaped.replace(/\n\n/g, '</p><p>');
            escaped = escaped.replace(/\n/g, '<br>');

            return '<p>' + escaped + '</p>';
        },

        persistHistory: function () {
            try {
                sessionStorage.setItem(STORAGE_KEY, JSON.stringify(this.history.slice(-10)));
            } catch (e) {
                // Ignore storage limits
            }
        },

        restoreSession: function () {
            try {
                const savedState = sessionStorage.getItem(STATE_KEY);
                if (savedState === "open") {
                    this.openChat();
                }

                const savedHistory = sessionStorage.getItem(STORAGE_KEY);
                if (savedHistory) {
                    const parsed = JSON.parse(savedHistory);
                    if (Array.isArray(parsed) && parsed.length > 0) {
                        this.history = parsed;
                        parsed.forEach(turn => {
                            if (turn.role === "user") {
                                this.appendUserMessage(turn.content);
                            } else if (turn.role === "assistant" || turn.role === "model") {
                                this.appendAssistantMessage(turn.content, [], []);
                            }
                        });
                    }
                }
            } catch (e) {
                // Ignore parse errors
            }
        }
    };

    // Auto-initialize when DOM is ready
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", () => Chatbot.init());
    } else {
        Chatbot.init();
    }

    // Expose globally
    window.PalaniMartChatbot = Chatbot;
})();
