package online.entreprenly.platform.sales.domain.model.queries;

/**
 * Query to retrieve the current connection status of the IoT scale.
 *
 * @param ownerEmail the authenticated account email making the request
 */
public record GetScaleStatusQuery(String ownerEmail) {
}

