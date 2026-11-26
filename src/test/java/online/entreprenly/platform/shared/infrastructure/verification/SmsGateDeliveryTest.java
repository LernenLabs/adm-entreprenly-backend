package online.entreprenly.platform.shared.infrastructure.verification;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class SmsGateDeliveryTest {
    @Test
    void submitsAnExpiringSmsToTheGatewayAndSurfacesQueueRejection() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var body = new AtomicReference<String>();
        var authorization = new AtomicReference<String>();
        var status = new AtomicInteger(202);
        server.createContext("/3rdparty/v1/messages", exchange -> {
            try (exchange) {
                body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                exchange.sendResponseHeaders(status.get(), -1);
            }
        });
        server.start();
        try {
            var mapper = new ObjectMapper();
            var endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/3rdparty/v1/messages");
            var gateway = new SmsGateDelivery(mapper, endpoint, "test-user", "test-pass", HttpClient.newHttpClient());
            var lifetime = Duration.ofMinutes(10);
            assertTrue(gateway.send("+51999000123", "012345", lifetime));
            var queued = mapper.readTree(body.get());
            assertEquals("+51999000123", queued.path("phoneNumbers").get(0).asText());
            assertEquals(600, queued.path("ttl").asInt());
            assertTrue(queued.path("textMessage").path("text").asText().contains("012345"));
            assertEquals("Basic dGVzdC11c2VyOnRlc3QtcGFzcw==", authorization.get());
            status.set(401);
            assertFalse(gateway.send("+51999000123", "012345", lifetime));
        } finally {
            server.stop(0);
        }
    }
}
