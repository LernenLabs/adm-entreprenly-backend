package online.entreprenly.platform.notification.support;

import online.entreprenly.platform.notification.application.internal.outboundservices.PushNotificationSender;
import online.entreprenly.platform.notification.domain.model.valueobjects.PushMessage;
import online.entreprenly.platform.notification.domain.model.valueobjects.PushResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecordingPushNotificationSender implements PushNotificationSender {

    public record Sent(String token, PushMessage message) {
    }

    private final List<Sent> sent = new ArrayList<>();
    private final Map<String, PushResult> resultsByToken = new HashMap<>();
    private PushResult defaultResult = PushResult.SENT;

    public void respondWith(String token, PushResult result) {
        resultsByToken.put(token, result);
    }

    public void respondWithByDefault(PushResult result) {
        defaultResult = result;
    }

    @Override
    public PushResult send(String deviceToken, PushMessage message) {
        sent.add(new Sent(deviceToken, message));
        return resultsByToken.getOrDefault(deviceToken, defaultResult);
    }

    public List<Sent> sent() {
        return sent;
    }
}
