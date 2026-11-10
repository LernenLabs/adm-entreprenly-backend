package online.entreprenly.platform.notification.application.internal.outboundservices;

import online.entreprenly.platform.notification.domain.model.valueobjects.PushMessage;
import online.entreprenly.platform.notification.domain.model.valueobjects.PushResult;

/**
 * Outbound port that delivers a push message to one device token.
 */
public interface PushNotificationSender {
    /**
     * Sends a message to a device.
     *
     * @param deviceToken the push registration token of the device
     * @param message     the message to deliver
     * @return the delivery outcome
     */
    PushResult send(String deviceToken, PushMessage message);
}
