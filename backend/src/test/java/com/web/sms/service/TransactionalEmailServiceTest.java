package com.web.sms.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.web.sms.exception.ServiceUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class TransactionalEmailServiceTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void sendsBrevoCompatibleRequestAndReturnsMessageId() throws Exception {
        AtomicReference<String> apiKey = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        startServer(exchange -> {
            apiKey.set(exchange.getRequestHeaders().getFirst("api-key"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 201, "{\"messageId\":\"brevo-message-123\"}");
        });

        String messageId = service("valid-brevo-key").sendHtml(
                "student@example.com", "OTP", "<p>Code: 123456</p>");

        assertEquals("brevo-message-123", messageId);
        assertEquals("valid-brevo-key", apiKey.get());
        assertTrue(requestBody.get().contains("student@example.com"));
        assertTrue(requestBody.get().contains("sender@example.com"));
        assertTrue(requestBody.get().contains("123456"));
    }

    @Test
    void convertsProviderRejectionToServiceUnavailable() throws Exception {
        startServer(exchange -> respond(exchange, 401, "{\"message\":\"Key not found\"}"));

        assertThrows(ServiceUnavailableException.class,
                () -> service("invalid-key").sendHtml("student@example.com", "OTP", "<p>123456</p>"));
    }

    @Test
    void rejectsSuccessfulResponseWithoutMessageId() throws Exception {
        startServer(exchange -> respond(exchange, 201, "{}"));

        assertThrows(ServiceUnavailableException.class,
                () -> service("valid-key").sendHtml("student@example.com", "OTP", "<p>123456</p>"));
    }

    private TransactionalEmailService service(String key) {
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/v3/smtp/email";
        return new TransactionalEmailService(mock(JavaMailSender.class), "BREVO_API", url, key,
                "", "sender@example.com", "LBRCE BUS");
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/smtp/email", exchange -> {
            try { handler.handle(exchange); }
            finally { exchange.close(); }
        });
        server.start();
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @FunctionalInterface
    private interface ExchangeHandler { void handle(HttpExchange exchange) throws IOException; }
}
