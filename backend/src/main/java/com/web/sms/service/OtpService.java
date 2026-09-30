package com.web.sms.service;

import com.web.sms.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** Proxies OTP calls so the provider API key is never delivered to a browser. */
@Service
public class OtpService {
    private final boolean enabled;
    private final String baseUrl;
    private final String apiKey;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public OtpService(@Value("${otp.enabled:false}") boolean enabled,
                      @Value("${otp.base-url:https://brevootpservice-production.up.railway.app}") String baseUrl,
                      @Value("${otp.api-key:}") String apiKey) {
        this.enabled = enabled; this.baseUrl = baseUrl.replaceAll("/$", ""); this.apiKey = apiKey;
    }

    public void send(String email) { call("/api/otp/send", Map.of("email", email)); }
    public void verify(String email, String otp) { call("/api/otp/verify", Map.of("email", email, "otp", otp)); }

    private void call(String path, Map<String, String> body) {
        if (!enabled || apiKey.isBlank()) throw new BadRequestException("Email OTP is not configured. Contact the transport administrator.");
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(15))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).header("X-API-Key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody(body))).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2 || !response.body().replaceAll("\\s", "").contains("\"success\":true")) {
                throw new BadRequestException("OTP request could not be completed");
            }
        } catch (BadRequestException e) { throw e;
        } catch (Exception e) { throw new BadRequestException("Unable to contact the email OTP service. Please try again."); }
    }

    private String jsonBody(Map<String, String> body) {
        return body.entrySet().stream().map(e -> "\"" + e.getKey() + "\":\"" + e.getValue().replace("\\", "\\\\").replace("\"", "\\\"") + "\"").collect(java.util.stream.Collectors.joining(",", "{", "}"));
    }
}
