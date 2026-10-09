package com.palani.palanimart.service.impl;

import com.palani.palanimart.dao.impl.CartDAOImpl;
import com.palani.palanimart.dao.impl.OrderDAOImpl;
import com.palani.palanimart.dao.impl.ProductDAOImpl;
import com.palani.palanimart.dto.ChatRequestDTO;
import com.palani.palanimart.dto.ChatResponseDTO;
import com.palani.palanimart.dto.PaginatedResult;
import com.palani.palanimart.dto.ProductDTO;
import com.palani.palanimart.exception.AppException;
import com.palani.palanimart.exception.ValidationException;
import com.palani.palanimart.model.Order;
import com.palani.palanimart.service.ChatService;
import com.palani.palanimart.service.OrderService;
import com.palani.palanimart.service.ProductService;
import com.palani.palanimart.service.ai.GeminiAiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Production-quality, dual-mode implementation of ChatService.
 * Connects to Google Gemini API when configured, with an intelligent,
 * catalog-grounded local fallback engine when offline or unconfigured.
 */
public class ChatServiceImpl implements ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);

    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");
    private static final Pattern CVV_PATTERN = Pattern.compile("\\b(?:cvv|cvc)\\s*[:=]?\\s*\\d{3,4}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("\\b(?:my password is|password:)\\s*\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE_BUDGET_PATTERN = Pattern.compile("(?:under|below|less than|within|max(?:imum)? of|budget(?: of)?)\\s*(?:₹|rs\\.?|\\$)?\\s*(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBER_BUDGET_PATTERN = Pattern.compile("(?:₹|rs\\.?|\\$)\\s*(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);

    private final ProductService productService;
    private final OrderService orderService;
    private final GeminiAiClient geminiClient;

    public ChatServiceImpl() {
        this(
                new ProductServiceImpl(new ProductDAOImpl()),
                new OrderServiceImpl(new OrderDAOImpl(), new CartDAOImpl(), new ProductDAOImpl()),
                new GeminiAiClient()
        );
    }

    public ChatServiceImpl(ProductService productService) {
        this(
                productService,
                new OrderServiceImpl(new OrderDAOImpl(), new CartDAOImpl(), new ProductDAOImpl()),
                new GeminiAiClient()
        );
    }

    public ChatServiceImpl(ProductService productService, OrderService orderService, GeminiAiClient geminiClient) {
        this.productService = productService;
        this.orderService = orderService;
        this.geminiClient = geminiClient != null ? geminiClient : new GeminiAiClient();
    }

    @Override
    public String processMessage(String userMessage, Long userId) throws AppException {
        ChatResponseDTO response = chat(new ChatRequestDTO(userMessage), userId);
        return response != null ? response.getReply() : "";
    }

    @Override
    public ChatResponseDTO chat(ChatRequestDTO request, Long userId) throws AppException {
        if (request == null || request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new ValidationException("Chat message cannot be empty", "EMPTY_MESSAGE");
        }

        String rawMessage = request.getMessage().trim();

        // Enforce input size limit
        if (rawMessage.length() > 1000) {
            throw new ValidationException("Message exceeds 1000 characters limit", "MESSAGE_TOO_LONG");
        }

        // Sensitive data protection (PCI/credentials defense)
        if (CREDIT_CARD_PATTERN.matcher(rawMessage).find() || CVV_PATTERN.matcher(rawMessage).find() || PASSWORD_PATTERN.matcher(rawMessage).find()) {
            logger.warn("Sensitive data pattern detected in chat query from user: {}", userId);
            return new ChatResponseDTO(
                    "⚠️ **Security Notice**: For your protection, never share credit card numbers, CVVs, passwords, or personal payment secrets in chat. PalaniMart uses a secure checkout flow for all transactions.",
                    Collections.emptyList(),
                    Arrays.asList("How does secure checkout work?", "What payment methods are supported?", "Return policy")
            );
        }

        logger.info("Processing chat query from user {}: {}", userId != null ? userId : "guest", rawMessage);

        // Attempt Gemini API if configured
        if (geminiClient != null && geminiClient.isConfigured()) {
            try {
                ChatResponseDTO aiResponse = generateGeminiChat(request, rawMessage, userId);
                if (aiResponse != null && aiResponse.getReply() != null && !aiResponse.getReply().trim().isEmpty()) {
                    return aiResponse;
                }
            } catch (Exception e) {
                logger.warn("Live Gemini API call failed, seamlessly falling back to catalog-grounded engine: {}", e.getMessage());
            }
        }

        // Local catalog-grounded intelligent fallback engine
        return processLocalGroundedChat(rawMessage, userId);
    }

    /**
     * Executes AI generation via Google Gemini, enriched with real catalog context and policies.
     */
    private ChatResponseDTO generateGeminiChat(ChatRequestDTO request, String rawMessage, Long userId) throws Exception {
        // Collect real catalog summary for grounding
        StringBuilder catalogContext = new StringBuilder();
        List<ProductDTO> sampleProducts = Collections.emptyList();
        try {
            PaginatedResult<ProductDTO> paginated = productService.getProductsPaginated(null, null, null, null, true, 1, 10, "id", "DESC");
            if (paginated != null && paginated.getData() != null) {
                sampleProducts = paginated.getData();
                catalogContext.append("Currently Featured PalaniMart Products:\n");
                for (ProductDTO p : sampleProducts) {
                    catalogContext.append(String.format("- ID: %d | Name: %s | Category: %s | Price: $%.2f | Stock: %d\n",
                            p.getId(), p.getName(), p.getCategory(), p.getPrice(), p.getStockQty()));
                }
            }
        } catch (Exception e) {
            logger.debug("Could not fetch sample products for Gemini prompt: {}", e.getMessage());
        }

        String userContext = (userId != null)
                ? "Customer is logged in with user ID: " + userId + "."
                : "Customer is currently a guest (not logged in). Prompt them to log in if they ask for personal order tracking.";

        String systemPrompt = "You are the PalaniMart AI Shopping Assistant, a friendly, accurate, and professional e-commerce guide.\n"
                + "PalaniMart Core Policies:\n"
                + "1. 7-day hassle-free return policy on eligible items (initiate on the Orders page).\n"
                + "2. Standard delivery is 2-4 business days across India.\n"
                + "3. Payments are protected with simulated escrow (held safely until delivery).\n"
                + "4. Support: support@palanimart.com.\n"
                + "5. Categories: Electronics, Books, Clothing, Home & Kitchen.\n"
                + userContext + "\n\n"
                + catalogContext.toString() + "\n"
                + "STRICT ACCURACY RULES:\n"
                + "- Never invent or hallucinate products, discounts, prices, or policies.\n"
                + "- If asked about order tracking, guide them to the Orders page.\n"
                + "- Keep your response concise, helpful, and formatted in clear markdown.\n";

        String reply = geminiClient.generateResponse(systemPrompt, request.getHistory(), rawMessage);

        // Extract products related to user query from the database
        List<ProductDTO> relevantProducts = findRelevantProducts(rawMessage);

        // Contextual follow-up suggestions
        List<String> suggestions = generateSuggestions(rawMessage, relevantProducts);

        return new ChatResponseDTO(reply, relevantProducts, suggestions);
    }

    /**
     * Intelligent, catalog-grounded local response engine.
     * Ensures robust operation without external AI dependencies or network outages.
     */
    private ChatResponseDTO processLocalGroundedChat(String rawMessage, Long userId) {
        String query = rawMessage.toLowerCase();

        // 1. Budget / Price Range Search (e.g. "under ₹1,000", "under $50", "below 100")
        BigDecimal budget = extractBudget(rawMessage);
        if (budget != null) {
            return handleBudgetQuery(budget, query);
        }

        // 2. Order Tracking ("where is my order", "order status", "track order")
        if (query.contains("order") && (query.contains("where") || query.contains("track") || query.contains("status") || query.contains("history") || query.contains("my"))) {
            return handleOrderTrackingQuery(userId);
        }

        // 3. Return & Refund Policy
        if (query.contains("return") || query.contains("refund")) {
            return new ChatResponseDTO(
                    "PalaniMart offers a **hassle-free 7-day return policy** on all eligible items. "
                            + "To initiate a return, go to your [Orders page](/palanimart/orders), select the delivered order, and request a return. "
                            + "Payments are held in escrow until delivery is verified, ensuring 100% purchase safety.",
                    Collections.emptyList(),
                    Arrays.asList("Track my orders", "Shipping policy", "Contact customer support")
            );
        }

        // 4. Shipping & Delivery
        if (query.contains("shipping") || query.contains("delivery") || query.contains("dispatch") || query.contains("how long")) {
            return new ChatResponseDTO(
                    "🚚 **Shipping & Delivery Policy**:\n"
                            + "- Standard delivery takes **2 to 4 business days** across India.\n"
                            + "- Verified sellers dispatch orders within 24–48 hours.\n"
                            + "- You can track real-time delivery progress directly in your [Orders tab](/palanimart/orders).",
                    Collections.emptyList(),
                    Arrays.asList("Track active orders", "Return policy", "Browse electronics")
            );
        }

        // 5. Payment, Security & Escrow
        if (query.contains("payment") || query.contains("pay") || query.contains("escrow") || query.contains("cod") || query.contains("card")) {
            return new ChatResponseDTO(
                    "💳 **Payment & Escrow Protection**:\n"
                            + "- PalaniMart operates a **simulated escrow payment model** where your payment is held securely until you receive your items.\n"
                            + "- We support simulated Net Banking, UPI, and Card checkout.\n"
                            + "- Your payment details are processed strictly via secure checkout—never in chat.",
                    Collections.emptyList(),
                    Arrays.asList("View shopping cart", "Return policy", "Browse popular items")
            );
        }

        // 6. Product Comparison ("compare", "which is better", "difference between")
        if (query.contains("compare") || query.contains("which is better") || query.contains("vs") || query.contains("difference")) {
            return handleComparisonQuery(query);
        }

        // 7. Category Queries (Electronics, Books, Clothing, Home)
        String matchedCategory = matchCategory(query);
        if (matchedCategory != null) {
            return handleCategoryQuery(matchedCategory);
        }

        // 8. Specific Product / Keyword Search
        List<ProductDTO> matchedProducts = findRelevantProducts(rawMessage);
        if (!matchedProducts.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Here are the matching products found in our catalog:\n\n");
            for (ProductDTO p : matchedProducts) {
                sb.append(String.format("- **%s** — **$%.2f** (%s, %s)\n",
                        p.getName(), p.getPrice(), p.getCategory(), p.getStockQty() > 0 ? "In Stock" : "Sold Out"));
            }
            sb.append("\nYou can click any product card below to view specifications or add items to your cart.");

            return new ChatResponseDTO(
                    sb.toString(),
                    matchedProducts,
                    Arrays.asList("Show items under $50", "Compare these products", "View return policy")
            );
        }

        // 9. Contact & Customer Support
        if (query.contains("contact") || query.contains("support") || query.contains("helpdesk") || query.contains("customer service")) {
            return new ChatResponseDTO(
                    "Our customer support team is available 24/7 to assist you. You can reach out directly via email at **support@palanimart.com**.",
                    Collections.emptyList(),
                    Arrays.asList("Track my order", "Return policy", "Browse all products")
            );
        }

        // 10. Greeting & Conversational Openers
        if (query.matches(".*\\b(hi|hello|hey|good morning|good afternoon|good evening|greetings)\\b.*") || query.equals("help")) {
            return new ChatResponseDTO(
                    "Hi! Welcome to PalaniMart. How can I help you shop today? I can help you search our catalog, check prices, compare products, track orders, or explain our return policies.",
                    Collections.emptyList(),
                    Arrays.asList("Show products under ₹1,000", "Best electronics", "Return policy", "Where is my order?")
            );
        }

        // 11. Clarifying Follow-up
        return new ChatResponseDTO(
                "I'm here to help you shop! I can search our catalog, recommend items by budget, compare products, check stock, or track orders. Could you specify the product name, category, or budget you're looking for?",
                Collections.emptyList(),
                Arrays.asList("Browse Electronics", "Browse Books", "Products under $50", "Return policy")
        );
    }

    private ChatResponseDTO handleBudgetQuery(BigDecimal budget, String query) {
        try {
            String category = matchCategory(query);
            PaginatedResult<ProductDTO> result = productService.getProductsPaginated(
                    null, category, null, budget, true, 1, 6, "price", "ASC");

            List<ProductDTO> products = (result != null && result.getData() != null) ? result.getData() : Collections.emptyList();

            if (products.isEmpty()) {
                return new ChatResponseDTO(
                        String.format("We couldn't find any in-stock items currently under $%.2f. Would you like to view our full catalog or check a slightly higher price range?", budget),
                        Collections.emptyList(),
                        Arrays.asList("Browse all products", "Show items under $100", "Best electronics")
                );
            }

            StringBuilder reply = new StringBuilder();
            reply.append(String.format("Here are the best in-stock products within your budget of **$%.2f**%s:\n\n",
                    budget, category != null ? " in *" + category + "*" : ""));

            for (ProductDTO p : products) {
                reply.append(String.format("- **%s** — **$%.2f** (%s)\n", p.getName(), p.getPrice(), p.getCategory()));
            }
            reply.append("\nClick on any product card below to view details or add directly to your cart.");

            return new ChatResponseDTO(
                    reply.toString(),
                    products,
                    Arrays.asList("Show cheaper options", "Compare top 2 items", "Return policy")
            );
        } catch (Exception e) {
            logger.error("Error executing budget search", e);
            return new ChatResponseDTO("Sorry, I encountered an issue searching products by budget. Please try again.", Collections.emptyList(), Collections.emptyList());
        }
    }

    private ChatResponseDTO handleOrderTrackingQuery(Long userId) {
        if (userId == null) {
            return new ChatResponseDTO(
                    "To track your orders, please [Sign In to your PalaniMart account](/palanimart/login). "
                            + "Once signed in, you can see real-time dispatch and delivery status for all your purchases.",
                    Collections.emptyList(),
                    Arrays.asList("Go to Login", "Return policy", "Browse products")
            );
        }

        try {
            List<Order> orders = orderService.getOrdersByBuyer(userId);
            if (orders == null || orders.isEmpty()) {
                return new ChatResponseDTO(
                        "You don't have any placed orders in your account right now. Ready to start shopping? Check out our featured products!",
                        Collections.emptyList(),
                        Arrays.asList("Browse electronics", "Browse books", "Products under $50")
                );
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Here are your recent orders:\n\n");
            int count = 0;
            for (Order o : orders) {
                if (count++ >= 3) break;
                sb.append(String.format("- **Order #%d** — Status: `%s` | Total: **$%.2f** ([View Order](/palanimart/orders?id=%d))\n",
                        o.getId(), o.getStatus().name(), o.getTotalAmount(), o.getId()));
            }
            sb.append("\nYou can view full delivery tracking and order items on your [Orders page](/palanimart/orders).");

            return new ChatResponseDTO(
                    sb.toString(),
                    Collections.emptyList(),
                    Arrays.asList("View full order history", "Return policy", "Continue shopping")
            );
        } catch (Exception e) {
            logger.error("Failed to fetch customer orders for user {}", userId, e);
            return new ChatResponseDTO(
                    "You can check your live order tracking updates directly in your [Orders dashboard](/palanimart/orders).",
                    Collections.emptyList(),
                    Arrays.asList("Go to Orders", "Return policy")
            );
        }
    }

    private ChatResponseDTO handleComparisonQuery(String query) {
        List<ProductDTO> matched = findRelevantProducts(query);
        if (matched.size() >= 2) {
            ProductDTO p1 = matched.get(0);
            ProductDTO p2 = matched.get(1);

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("### Comparison: %s vs %s\n\n", p1.getName(), p2.getName()));
            sb.append(String.format("- **Price**: %s is **$%.2f** vs %s at **$%.2f**\n",
                    p1.getName(), p1.getPrice(), p2.getName(), p2.getPrice()));
            sb.append(String.format("- **Category**: %s vs %s\n", p1.getCategory(), p2.getCategory()));
            sb.append(String.format("- **Stock Status**: %s (%d left) vs %s (%d left)\n",
                    p1.getStockQty() > 0 ? "In Stock" : "Sold Out", p1.getStockQty(),
                    p2.getStockQty() > 0 ? "In Stock" : "Sold Out", p2.getStockQty()));
            sb.append(String.format("- **%s Summary**: %s\n", p1.getName(), p1.getDescription()));
            sb.append(String.format("- **%s Summary**: %s\n\n", p2.getName(), p2.getDescription()));

            if (p1.getPrice().compareTo(p2.getPrice()) < 0) {
                sb.append(String.format("💡 **%s** is more budget-friendly by $%.2f.",
                        p1.getName(), p2.getPrice().subtract(p1.getPrice())));
            } else if (p2.getPrice().compareTo(p1.getPrice()) < 0) {
                sb.append(String.format("💡 **%s** is more budget-friendly by $%.2f.",
                        p2.getName(), p1.getPrice().subtract(p2.getPrice())));
            }

            return new ChatResponseDTO(sb.toString(), Arrays.asList(p1, p2), Arrays.asList("Add " + p1.getName() + " to cart", "Add " + p2.getName() + " to cart", "Return policy"));
        }

        return new ChatResponseDTO(
                "Which specific products would you like to compare? You can name any two products in our catalog (e.g., 'Compare Mechanical Keyboard and Headphones').",
                Collections.emptyList(),
                Arrays.asList("Compare Keyboard & Headphones", "Compare Clean Code & Effective Java", "Browse catalog")
        );
    }

    private ChatResponseDTO handleCategoryQuery(String category) {
        try {
            PaginatedResult<ProductDTO> result = productService.getProductsPaginated(
                    null, category, null, null, true, 1, 6, "id", "DESC");
            List<ProductDTO> products = (result != null && result.getData() != null) ? result.getData() : Collections.emptyList();

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Here are top items in our **%s** collection:\n\n", category));
            for (ProductDTO p : products) {
                sb.append(String.format("- **%s** — **$%.2f** (%s)\n", p.getName(), p.getPrice(), p.getStockQty() > 0 ? "In Stock" : "Sold Out"));
            }
            sb.append(String.format("\nBrowse the full collection on the [Product Catalog](/palanimart/products?category=%s).", category));

            return new ChatResponseDTO(
                    sb.toString(),
                    products,
                    Arrays.asList("Filter under $50", "View other categories", "Return policy")
            );
        } catch (Exception e) {
            logger.error("Category query error", e);
            return new ChatResponseDTO("Browse all items in " + category + " on our [Product Page](/palanimart/products?category=" + category + ").", Collections.emptyList(), Collections.emptyList());
        }
    }

    private List<ProductDTO> findRelevantProducts(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            // First try keyword search across whole catalog
            String keyword = extractKeywords(text);
            if (!keyword.isEmpty()) {
                List<ProductDTO> list = productService.searchProducts(null, keyword, 1, 6);
                if (list != null && !list.isEmpty()) {
                    return list;
                }
            }

            // Fallback: check category matches
            String category = matchCategory(text);
            if (category != null) {
                return productService.searchProducts(category, null, 1, 6);
            }
        } catch (Exception e) {
            logger.debug("Error finding relevant products: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private String extractKeywords(String text) {
        String clean = text.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", " ").trim();
        List<String> stopWords = Set.of("the", "a", "an", "is", "are", "and", "or", "to", "for", "in", "of", "on", "with",
                "show", "me", "find", "get", "what", "which", "where", "how", "i", "want", "need", "buy", "looking", "please",
                "can", "you", "tell", "best", "good", "item", "items", "product", "products").stream().toList();

        String[] tokens = clean.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (token.length() > 2 && !stopWords.contains(token)) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(token);
            }
        }
        return sb.toString();
    }

    private BigDecimal extractBudget(String text) {
        Matcher m1 = PRICE_BUDGET_PATTERN.matcher(text);
        if (m1.find()) {
            try {
                return new BigDecimal(m1.group(1));
            } catch (Exception ignored) {
            }
        }
        Matcher m2 = NUMBER_BUDGET_PATTERN.matcher(text);
        if (m2.find()) {
            try {
                return new BigDecimal(m2.group(1));
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String matchCategory(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("electronic") || lower.contains("gadget") || lower.contains("phone") || lower.contains("keyboard") || lower.contains("headphone") || lower.contains("speaker") || lower.contains("hub")) {
            return "Electronics";
        }
        if (lower.contains("book") || lower.contains("reading") || lower.contains("novel") || lower.contains("code") || lower.contains("java")) {
            return "Books";
        }
        if (lower.contains("cloth") || lower.contains("apparel") || lower.contains("shirt") || lower.contains("wear") || lower.contains("dress") || lower.contains("fashion")) {
            return "Clothing";
        }
        if (lower.contains("home") || lower.contains("kitchen") || lower.contains("mug") || lower.contains("cook") || lower.contains("desk") || lower.contains("furniture")) {
            return "Home";
        }
        return null;
    }

    private List<String> generateSuggestions(String query, List<ProductDTO> products) {
        List<String> suggestions = new ArrayList<>();
        if (products != null && !products.isEmpty()) {
            suggestions.add("Compare these items");
            suggestions.add("Check return policy");
            suggestions.add("View my orders");
        } else {
            suggestions.add("Show products under $50");
            suggestions.add("Best electronics");
            suggestions.add("Return policy");
        }
        return suggestions;
    }
}
