package online.entreprenly.platform.sales.domain.model.queries;

/**
 * Query to retrieve the current weight reading from the IoT scale.
 *
 * @param ownerEmail the authenticated account email making the request
 */
public record GetScaleWeightQuery(String ownerEmail) {
}
