package online.entreprenly.platform.shared.infrastructure.verification;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
public class SmsGateDelivery {
    private final ObjectMapper json;
    private final HttpClient client;
    private final URI endpoint;
    private final String authorization;

    @Autowired
    public SmsGateDelivery(ObjectMapper json,
            @Value("${SMSGATE_MESSAGES_URL:https://api.sms-gate.app/3rdparty/v1/messages}") String url,
            @Value("${SMSGATE_USERNAME:}") String username, @Value("${SMSGATE_PASSWORD:}") String password) {
        this(json, httpsEndpoint(url), username, password,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    SmsGateDelivery(ObjectMapper json, URI endpoint, String username, String password, HttpClient client) {
        this.json = json;
        this.client = client;
        this.endpoint = endpoint;
        this.authorization = username.isBlank() || password.isBlank() ? null
                : "Basic " + Base64.getEncoder().encodeToString((username + ":" + password)
                        .getBytes(StandardCharsets.UTF_8));
    }

    boolean send(String phone, String code, Duration lifetime) {
        if (endpoint == null || authorization == null) return false;
        // Keep this GSM-7 to fit a single SMS. The code is never written to logs.
        String message = "Entreprenly: tu codigo es " + code + ". Vence en 10 minutos. No lo compartas.";
        try {
            var body = json.writeValueAsString(Map.of(
                    "textMessage", Map.of("text", message),
                    "phoneNumbers", List.of(phone),
                    "ttl", lifetime.toSeconds()));
            var request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(8))
                    .header("Authorization", authorization)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status >= 200 && status < 300) return true;
            log.warn("SMSGate rejected verification SMS (HTTP {}).", status);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException ex) {
            log.warn("SMSGate verification SMS request could not be completed.");
        }
        return false;
    }

    private static URI httpsEndpoint(String value) {
        try {
            var uri = URI.create(value.trim());
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && uri.getUserInfo() == null && uri.getFragment() == null ? uri : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
