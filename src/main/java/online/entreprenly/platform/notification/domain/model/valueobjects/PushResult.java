package online.entreprenly.platform.notification.domain.model.valueobjects;

/**
 * Outcome of delivering a push message to one device token.
 */
public enum PushResult {
    /** The push provider accepted the message. */
    SENT,
    /** The provider reported the token as unregistered or malformed; it should be removed. */
    INVALID_TOKEN,
    /** The push provider is not configured in this environment, so nothing was sent. */
    NOT_CONFIGURED,
    /** A transient failure; the delivery can be retried on the next run. */
    FAILED
}
