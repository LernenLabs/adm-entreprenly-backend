package online.entreprenly.platform.notification.infrastructure.messaging.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import online.entreprenly.platform.notification.application.internal.outboundservices.PushNotificationSender;
import online.entreprenly.platform.notification.domain.model.valueobjects.PushMessage;
import online.entreprenly.platform.notification.domain.model.valueobjects.PushResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sends push notifications through the Firebase Cloud Messaging HTTP v1 API.
 *
 * <p>Authenticates with a service account (OAuth2 JWT bearer flow). When FCM is disabled or the
 * credentials are missing it logs the message and reports {@link PushResult#NOT_CONFIGURED}, so
 * the backend keeps working in environments without Firebase (mirrors the WhatsApp bridge
 * adapter).</p>
 */
@Service
public class FcmPushNotificationSender implements PushNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(FcmPushNotificationSender.class);

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    private final boolean enabled;
    private final String projectId;
    private final String clientEmail;
    private final String privateKeyPem;
    private final String channelId;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private String cachedAccessToken;
    private Instant cachedAccessTokenExpiresAt = Instant.EPOCH;

    public FcmPushNotificationSender(
            @Value("${notifications.push.fcm.enabled:false}") boolean enabled,
            @Value("${notifications.push.fcm.project-id:}") String projectId,
            @Value("${notifications.push.fcm.client-email:}") String clientEmail,
            @Value("${notifications.push.fcm.private-key:}") String privateKeyPem,
            @Value("${notifications.push.fcm.channel-id:lot_expiry}") String channelId) {
        this.enabled = enabled;
        this.projectId = projectId;
        this.clientEmail = clientEmail;
        this.privateKeyPem = privateKeyPem;
        this.channelId = channelId;
    }

    @Override
    public PushResult send(String deviceToken, PushMessage message) {
        if (!isConfigured()) {
            log.info("[notification] FCM not configured; push '{}' not delivered", message.title());
            return PushResult.NOT_CONFIGURED;
        }
        try {
            var request = HttpRequest.newBuilder(URI.create(
                            "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + accessToken())
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(buildBody(deviceToken, message)))
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return interpret(response.statusCode(), response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[notification] FCM send interrupted");
            return PushResult.FAILED;
        } catch (Exception e) {
            log.warn("[notification] FCM send failed: {}", e.getMessage());
            return PushResult.FAILED;
        }
    }

    private boolean isConfigured() {
        return enabled && !projectId.isBlank() && !clientEmail.isBlank() && !privateKeyPem.isBlank();
    }

    private PushResult interpret(int status, String body) {
        if (status >= 200 && status < 300) {
            return PushResult.SENT;
        }
        if (status == 404 || (body != null && body.contains("UNREGISTERED"))) {
            return PushResult.INVALID_TOKEN;
        }
        if (status == 401) {
            synchronized (this) {
                cachedAccessToken = null;
            }
        }
        log.warn("[notification] FCM responded {}: {}", status, body);
        return PushResult.FAILED;
    }

    private String buildBody(String deviceToken, PushMessage message) throws Exception {
        var notification = new LinkedHashMap<String, Object>();
        notification.put("title", message.title());
        notification.put("body", message.body());

        var androidNotification = new LinkedHashMap<String, Object>();
        androidNotification.put("channel_id", channelId);

        var android = new LinkedHashMap<String, Object>();
        android.put("priority", "HIGH");
        android.put("notification", androidNotification);

        var fcmMessage = new LinkedHashMap<String, Object>();
        fcmMessage.put("token", deviceToken);
        fcmMessage.put("notification", notification);
        fcmMessage.put("data", message.data());
        fcmMessage.put("android", android);

        return objectMapper.writeValueAsString(Map.of("message", fcmMessage));
    }

    private synchronized String accessToken() throws Exception {
        if (cachedAccessToken != null && Instant.now().isBefore(cachedAccessTokenExpiresAt)) {
            return cachedAccessToken;
        }
        var now = Instant.now();
        var assertion = Jwts.builder()
                .issuer(clientEmail)
                .subject(clientEmail)
                .claim("scope", SCOPE)
                .claim("aud", TOKEN_URL)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(parsePrivateKey())
                .compact();

        var form = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", StandardCharsets.UTF_8)
                + "&assertion=" + URLEncoder.encode(assertion, StandardCharsets.UTF_8);
        var request = HttpRequest.newBuilder(URI.create(TOKEN_URL))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("Google OAuth responded " + response.statusCode());
        }
        var json = objectMapper.readTree(response.body());
        cachedAccessToken = json.path("access_token").asText();
        long expiresIn = json.path("expires_in").asLong(3600);
        cachedAccessTokenExpiresAt = now.plusSeconds(Math.max(60, expiresIn - 60));
        return cachedAccessToken;
    }

    private PrivateKey parsePrivateKey() throws Exception {
        var pem = privateKeyPem
                .replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        var der = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }
}
