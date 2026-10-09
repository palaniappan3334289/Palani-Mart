package com.palani.palanimart.controller;

import com.palani.palanimart.dto.ChatRequestDTO;
import com.palani.palanimart.dto.ChatResponseDTO;
import com.palani.palanimart.dto.ProductDTO;
import com.palani.palanimart.dto.UserResponseDTO;
import com.palani.palanimart.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServletTest {

    @Mock
    private ChatService chatService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    private ChatServlet servlet;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        servlet = new ChatServlet(chatService);
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    @DisplayName("POST /api/chat with valid message returns AI assistant response")
    void testChatValidMessage() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        UserResponseDTO user = new UserResponseDTO();
        user.setId(10L);
        when(session.getAttribute("currentUser")).thenReturn(user);

        String json = "{\"message\":\"What is your return policy?\"}";
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(json)));
        when(chatService.processMessage(eq("What is your return policy?"), eq(10L)))
                .thenReturn("We offer a 30-day hassle-free return policy.");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String jsonOutput = responseWriter.toString();
        assertTrue(jsonOutput.contains("\"success\":true"));
        assertTrue(jsonOutput.contains("30-day hassle-free return policy"));
    }

    @Test
    @DisplayName("POST /api/chat with empty message returns 400 validation error")
    void testChatEmptyMessageReturns400() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        String json = "{\"message\":\"\"}";
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(json)));

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String jsonOutput = responseWriter.toString();
        assertTrue(jsonOutput.contains("\"success\":false"));
        assertTrue(jsonOutput.contains("EMPTY_MESSAGE"));
    }

    @Test
    @DisplayName("POST /api/chat with message > 1000 chars returns 400 validation error")
    void testChatLongMessageReturns400() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        String longMsg = "x".repeat(1005);
        String json = "{\"message\":\"" + longMsg + "\"}";
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(json)));

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String jsonOutput = responseWriter.toString();
        assertTrue(jsonOutput.contains("\"success\":false"));
        assertTrue(jsonOutput.contains("MESSAGE_TOO_LONG"));
    }

    @Test
    @DisplayName("POST /api/chat with rich products and suggestions returns structured JSON envelope")
    void testChatWithRichResponse() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        UserResponseDTO user = new UserResponseDTO();
        user.setId(5L);
        when(session.getAttribute("currentUser")).thenReturn(user);

        ProductDTO p = new ProductDTO(1L, 2L, "Mechanical Keyboard", "RGB Gaming", new BigDecimal("89.99"), 10, "Electronics", "https://img.jpg");
        ChatResponseDTO richResp = new ChatResponseDTO("Found 1 product", Collections.singletonList(p), Collections.singletonList("Compare items"));

        when(chatService.chat(any(ChatRequestDTO.class), eq(5L))).thenReturn(richResp);

        String json = "{\"message\":\"Show me keyboards\"}";
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(json)));

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String jsonOutput = responseWriter.toString();
        assertTrue(jsonOutput.contains("\"success\":true"));
        assertTrue(jsonOutput.contains("Mechanical Keyboard"));
        assertTrue(jsonOutput.contains("Compare items"));
    }

    @Test
    @DisplayName("POST /api/chat rate limit triggers 429 when client sends excessive requests")
    void testChatRateLimiting() throws Exception {
        when(request.getRemoteAddr()).thenReturn("192.168.1.99");
        when(request.getSession(false)).thenReturn(null);

        String json = "{\"message\":\"Hi\"}";
        when(chatService.processMessage(anyString(), any())).thenReturn("Hello");

        // Send 31 requests from same IP
        for (int i = 0; i < 30; i++) {
            when(request.getReader()).thenReturn(new BufferedReader(new StringReader(json)));
            servlet.doPost(request, response);
        }

        // 31st request should be rate-limited
        StringWriter rateLimitedWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(rateLimitedWriter));
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(json)));

        servlet.doPost(request, response);

        verify(response, atLeastOnce()).setStatus(429);
        String jsonOutput = rateLimitedWriter.toString();
        assertTrue(jsonOutput.contains("TOO_MANY_REQUESTS"));
    }
}
