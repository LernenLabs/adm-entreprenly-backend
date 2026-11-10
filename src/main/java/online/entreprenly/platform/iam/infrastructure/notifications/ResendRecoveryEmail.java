package online.entreprenly.platform.iam.infrastructure.notifications;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
public class ResendRecoveryEmail {
    private static final URI ENDPOINT = URI.create("https://api.resend.com/emails");
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json;
    private final String apiKey;
    private final String sender;
    private final URI publicBase;

    public ResendRecoveryEmail(ObjectMapper json,
            @Value("${RESEND_API_KEY:}") String apiKey,
            @Value("${RESEND_FROM_EMAIL:}") String sender,
            @Value("${PUBLIC_API_URL:}") String publicApiUrl) {
        this.json = json;
        this.apiKey = apiKey.trim();
        this.sender = sender.trim();
        this.publicBase = httpsOrigin(publicApiUrl);
    }

    public boolean available() {
        return !apiKey.isBlank() && sender.toLowerCase(Locale.ROOT).endsWith("@entreprenly.online")
                && publicBase != null;
    }

    public String link(String path) {
        return publicBase.resolve(path).toString();
    }

    public void send(String recipient, String link) {
        try {
            var request = HttpRequest.newBuilder(ENDPOINT)
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of(
                            "from", sender,
                            "to", List.of(recipient),
                            "subject", "Recupera tu contraseña de Entreprenly",
                            "text", "Abre este enlace para restablecer tu contraseña de Entreprenly. "
                                    + "Vence en 10 minutos. Si no lo solicitaste, ignora este correo.\n\n" + link))))
                    .build();
            client.sendAsync(request, HttpResponse.BodyHandlers.discarding()).whenComplete((response, error) -> {
                if (error != null) log.warn("Could not reach Resend for a password recovery email.");
                else if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    log.warn("Resend rejected a password recovery email (status {}).", response.statusCode());
                }
            });
        } catch (RuntimeException ex) {
            log.warn("Could not prepare the Resend password recovery request.");
        }
    }

    private static URI httpsOrigin(String value) {
        try {
            URI uri = URI.create(value.trim());
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null
                    && uri.getQuery() == null && uri.getFragment() == null ? uri : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
