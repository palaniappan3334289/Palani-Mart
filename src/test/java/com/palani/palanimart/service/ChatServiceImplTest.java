package com.palani.palanimart.service;

import com.palani.palanimart.dto.ChatRequestDTO;
import com.palani.palanimart.dto.ChatResponseDTO;
import com.palani.palanimart.dto.PaginatedResult;
import com.palani.palanimart.dto.ProductDTO;
import com.palani.palanimart.exception.ValidationException;
import com.palani.palanimart.model.Order;
import com.palani.palanimart.model.OrderStatus;
import com.palani.palanimart.service.ai.GeminiAiClient;
import com.palani.palanimart.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceImplTest {

    @Mock
    private ProductService productService;

    @Mock
    private OrderService orderService;

    @Mock
    private GeminiAiClient geminiClient;

    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatServiceImpl(productService, orderService, geminiClient);
    }

    @Test
    @DisplayName("Blank or null chat message throws ValidationException")
    void testBlankMessageThrowsValidationException() {
        assertThrows(ValidationException.class, () -> chatService.chat(new ChatRequestDTO("   "), 1L));
        assertThrows(ValidationException.class, () -> chatService.chat(null, 1L));
    }

    @Test
    @DisplayName("Message exceeding 1000 characters throws ValidationException")
    void testOversizedMessageThrowsValidationException() {
        String longText = "a".repeat(1001);
        assertThrows(ValidationException.class, () -> chatService.chat(new ChatRequestDTO(longText), 1L));
    }

    @Test
    @DisplayName("Credit card pattern triggers security notice and avoids processing")
    void testCreditCardSecurityNotice() throws Exception {
        ChatRequestDTO request = new ChatRequestDTO("My card is 4111 2222 3333 4444 and cvv 123");
        ChatResponseDTO response = chatService.chat(request, 1L);

        assertNotNull(response);
        assertTrue(response.getReply().contains("Security Notice"));
        assertTrue(response.getReply().contains("never share credit card"));
    }

    @Test
    @DisplayName("Return policy inquiry returns accurate 7-day policy details")
    void testReturnPolicyQuery() throws Exception {
        ChatRequestDTO request = new ChatRequestDTO("What is your return policy?");
        ChatResponseDTO response = chatService.chat(request, 1L);

        assertNotNull(response);
        assertTrue(response.getReply().contains("7-day return policy"));
        assertTrue(response.getReply().contains("/palanimart/orders"));
    }

    @Test
    @DisplayName("Shipping inquiry returns 2-4 business days delivery info")
    void testShippingPolicyQuery() throws Exception {
        ChatRequestDTO request = new ChatRequestDTO("How long does shipping take?");
        ChatResponseDTO response = chatService.chat(request, 1L);

        assertNotNull(response);
        assertTrue(response.getReply().contains("2 to 4 business days"));
    }

    @Test
    @DisplayName("Budget search under $100 queries catalog and returns product cards")
    void testBudgetQuery() throws Exception {
        ProductDTO p1 = new ProductDTO(1L, 2L, "Mechanical Keyboard", "Cherry MX switches", new BigDecimal("89.99"), 25, "Electronics", "https://img.example/1.jpg");
        PaginatedResult<ProductDTO> result = PaginatedResult.of(Collections.singletonList(p1), 1, 6, 1);

        when(productService.getProductsPaginated(isNull(), isNull(), isNull(), eq(new BigDecimal("100")), eq(true), eq(1), eq(6), eq("price"), eq("ASC")))
                .thenReturn(result);

        ChatRequestDTO request = new ChatRequestDTO("Show me products under $100");
        ChatResponseDTO response = chatService.chat(request, null);

        assertNotNull(response);
        assertFalse(response.getProducts().isEmpty());
        assertEquals("Mechanical Keyboard", response.getProducts().get(0).getName());
        assertTrue(response.getReply().contains("Mechanical Keyboard"));
    }

    @Test
    @DisplayName("Order tracking as guest prompts to sign in")
    void testOrderTrackingAsGuest() throws Exception {
        ChatRequestDTO request = new ChatRequestDTO("Where is my order?");
        ChatResponseDTO response = chatService.chat(request, null);

        assertNotNull(response);
        assertTrue(response.getReply().contains("Sign In to your PalaniMart account"));
        assertTrue(response.getReply().contains("/palanimart/login"));
    }

    @Test
    @DisplayName("Order tracking as logged in buyer returns their order status")
    void testOrderTrackingAsAuthenticatedBuyer() throws Exception {
        Order o = new Order();
        o.setId(101L);
        o.setBuyerId(5L);
        o.setStatus(OrderStatus.CONFIRMED);
        o.setTotalAmount(new BigDecimal("149.50"));

        when(orderService.getOrdersByBuyer(eq(5L))).thenReturn(Collections.singletonList(o));

        ChatRequestDTO request = new ChatRequestDTO("Where is my order?");
        ChatResponseDTO response = chatService.chat(request, 5L);

        assertNotNull(response);
        assertTrue(response.getReply().contains("Order #101"));
        assertTrue(response.getReply().contains("CONFIRMED"));
    }

    @Test
    @DisplayName("Live Gemini AI response is returned when client is configured")
    void testLiveGeminiResponseWhenConfigured() throws Exception {
        when(geminiClient.isConfigured()).thenReturn(true);
        when(geminiClient.generateResponse(anyString(), any(), eq("Tell me about keyboards")))
                .thenReturn("PalaniMart offers premium mechanical keyboards with tactile switches.");

        ChatRequestDTO request = new ChatRequestDTO("Tell me about keyboards");
        ChatResponseDTO response = chatService.chat(request, 1L);

        assertNotNull(response);
        assertEquals("PalaniMart offers premium mechanical keyboards with tactile switches.", response.getReply());
    }

    @Test
    @DisplayName("Fallback to local engine occurs gracefully if Gemini throws error")
    void testGeminiFallbackToLocalEngineOnException() throws Exception {
        when(geminiClient.isConfigured()).thenReturn(true);
        when(geminiClient.generateResponse(anyString(), any(), anyString()))
                .thenThrow(new RuntimeException("Gemini quota exceeded 429"));

        ChatRequestDTO request = new ChatRequestDTO("What is your refund policy?");
        ChatResponseDTO response = chatService.chat(request, 1L);

        assertNotNull(response);
        assertTrue(response.getReply().contains("7-day return policy"));
    }
}
