package online.entreprenly.platform.sales.domain.model.valueobjects;

/**
 * Connection status of the IoT scale peripheral.
 *
 * <p>CONNECTED means the scale is reachable and ready to provide weight readings.
 * DISCONNECTED means the scale cannot be reached or the last heartbeat timed out.</p>
 */
public enum ScaleStatus {
    CONNECTED,
    DISCONNECTED
}

