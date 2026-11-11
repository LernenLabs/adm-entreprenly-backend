package online.entreprenly.platform.sales.application.internal.queryservices;

import online.entreprenly.platform.sales.application.queryservices.IotScaleQueryService;
import online.entreprenly.platform.sales.domain.model.queries.GetScaleStatusQuery;
import online.entreprenly.platform.sales.domain.model.queries.GetScaleWeightQuery;
import online.entreprenly.platform.sales.domain.model.valueobjects.ScaleStatus;
import online.entreprenly.platform.shared.application.result.ApplicationError;
import online.entreprenly.platform.shared.application.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * IoT scale query service implementation.
 *
 * <p>Calls the IoT scale bridge at the configured base URL to retrieve connection
 * status and weight readings. When the bridge is unreachable the service degrades
 * gracefully: status returns DISCONNECTED and weight returns a business-rule error
 * so the Android app can offer the manual-entry fallback.</p>
 *
 * <p>When {@code iot.scale.base-url} is blank (the default for dev/test) the
 * service returns a DISCONNECTED status and a stub weight of 0.0 kg so that
 * the rest of the stack can be exercised without hardware.</p>
 */
@Service
public class IotScaleQueryServiceImpl implements IotScaleQueryService {

    private static final Logger log = LoggerFactory.getLogger(IotScaleQueryServiceImpl.class);

    /** Path of the status sub-resource on the scale bridge. */
    private static final String STATUS_PATH = "/status";

    /** Path of the weight reading sub-resource on the scale bridge. */
    private static final String WEIGHT_PATH = "/weight";

    private final String scaleBaseUrl;
    private final RestClient restClient;

    public IotScaleQueryServiceImpl(
            @Value("${iot.scale.base-url:}") String scaleBaseUrl,
            RestClient.Builder restClientBuilder) {
        this.scaleBaseUrl = scaleBaseUrl == null ? "" : scaleBaseUrl.strip();
        this.restClient  = restClientBuilder.build();
    }

    // -------------------------------------------------------------------------
    // GetScaleStatusQuery
    // -------------------------------------------------------------------------

    @Override
    public Result<ScaleStatus, ApplicationError> handle(GetScaleStatusQuery query) {
        if (query.ownerEmail() == null || query.ownerEmail().isBlank()) {
            return Result.failure(ApplicationError.validationError("ownerEmail", "An authenticated owner is required"));
        }

        if (scaleBaseUrl.isEmpty()) {
            log.debug("[iot-scale] no bridge URL configured – returning DISCONNECTED stub");
            return Result.success(ScaleStatus.DISCONNECTED);
        }

        try {
            var response = restClient
                    .get()
                    .uri(scaleBaseUrl + STATUS_PATH)
                    .retrieve()
                    .body(BridgeStatusResponse.class);

            if (response == null) {
                return Result.success(ScaleStatus.DISCONNECTED);
            }
            var status = Boolean.TRUE.equals(response.connected())
                    ? ScaleStatus.CONNECTED
                    : ScaleStatus.DISCONNECTED;
            return Result.success(status);

        } catch (ResourceAccessException ex) {
            log.warn("[iot-scale] scale bridge unreachable: {}", ex.getMessage());
            return Result.success(ScaleStatus.DISCONNECTED);
        } catch (Exception ex) {
            log.error("[iot-scale] unexpected error retrieving scale status", ex);
            return Result.failure(ApplicationError.unexpected("iot-scale-status", ex.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // GetScaleWeightQuery
    // -------------------------------------------------------------------------

    @Override
    public Result<Double, ApplicationError> handle(GetScaleWeightQuery query) {
        if (query.ownerEmail() == null || query.ownerEmail().isBlank()) {
            return Result.failure(ApplicationError.validationError("ownerEmail", "An authenticated owner is required"));
        }

        if (scaleBaseUrl.isEmpty()) {
            log.debug("[iot-scale] no bridge URL configured – returning 0.0 kg stub");
            return Result.success(0.0);
        }

        try {
            var response = restClient
                    .get()
                    .uri(scaleBaseUrl + WEIGHT_PATH)
                    .retrieve()
                    .body(BridgeWeightResponse.class);

            if (response == null) {
                return Result.failure(ApplicationError.businessRuleViolation(
                        "scale-disconnected", "No weight reading available; scale may be disconnected"));
            }

            double weightKg = response.weightKg();
            if (weightKg < 0) {
                return Result.failure(ApplicationError.validationError(
                        "weightKg", "Scale returned a negative weight reading: " + weightKg));
            }
            return Result.success(Math.round(weightKg * 1000.0) / 1000.0);

        } catch (ResourceAccessException ex) {
            log.warn("[iot-scale] scale bridge unreachable when reading weight: {}", ex.getMessage());
            return Result.failure(ApplicationError.businessRuleViolation(
                    "scale-disconnected", "Scale is not reachable; use manual weight entry"));
        } catch (Exception ex) {
            log.error("[iot-scale] unexpected error retrieving weight reading", ex);
            return Result.failure(ApplicationError.unexpected("iot-scale-weight", ex.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // Bridge DTO projections (private – not exposed outside this class)
    // -------------------------------------------------------------------------

    /** JSON shape returned by the bridge /status endpoint. */
    private record BridgeStatusResponse(Boolean connected) {}

    /** JSON shape returned by the bridge /weight endpoint. */
    private record BridgeWeightResponse(double weightKg) {}
}

