package online.entreprenly.platform.sales.application;

import online.entreprenly.platform.sales.application.internal.queryservices.IotScaleQueryServiceImpl;
import online.entreprenly.platform.sales.domain.model.queries.GetScaleStatusQuery;
import online.entreprenly.platform.sales.domain.model.queries.GetScaleWeightQuery;
import online.entreprenly.platform.sales.domain.model.valueobjects.ScaleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link IotScaleQueryServiceImpl}.
 *
 * <p>These tests exercise the stub / no-bridge-URL path, validating that:
 * <ul>
 *   <li>DISCONNECTED is returned when no bridge URL is configured (dev/test stub).</li>
 *   <li>0.0 kg is returned when no bridge URL is configured (dev/test stub).</li>
 *   <li>Validation errors are returned when ownerEmail is blank.</li>
 * </ul>
 * Integration tests with a live mock bridge are covered separately via Wiremock.</p>
 */
class IotScaleQueryServiceImplTest {

    private static final String OWNER = "seller@shop.com";

    private IotScaleQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        // No bridge URL configured → stub / graceful-degradation mode
        service = new IotScaleQueryServiceImpl("", RestClient.builder());
    }

    // -------------------------------------------------------------------------
    // GetScaleStatusQuery – stub mode (no bridge URL)
    // -------------------------------------------------------------------------

    @Test
    void statusReturnsDisconnectedWhenNoBridgeUrlIsConfigured() {
        var result = service.handle(new GetScaleStatusQuery(OWNER));

        assertTrue(result.isSuccess(), "Expected success but got: " + result);
        assertEquals(ScaleStatus.DISCONNECTED, result.toOptional().orElseThrow());
    }

    @Test
    void statusReturnsValidationErrorWhenOwnerEmailIsBlank() {
        var result = service.handle(new GetScaleStatusQuery("   "));

        assertTrue(result.isFailure(), "Expected failure but got success");
        var error = ((online.entreprenly.platform.shared.application.result.Result.Failure<?, ?>) result).error();
        assertEquals("VALIDATION_ERROR", ((online.entreprenly.platform.shared.application.result.ApplicationError) error).code());
    }

    @Test
    void statusReturnsValidationErrorWhenOwnerEmailIsNull() {
        var result = service.handle(new GetScaleStatusQuery(null));

        assertTrue(result.isFailure());
    }

    // -------------------------------------------------------------------------
    // GetScaleWeightQuery – stub mode (no bridge URL)
    // -------------------------------------------------------------------------

    @Test
    void weightReturnsZeroKgWhenNoBridgeUrlIsConfigured() {
        var result = service.handle(new GetScaleWeightQuery(OWNER));

        assertTrue(result.isSuccess(), "Expected success but got: " + result);
        assertEquals(0.0, result.toOptional().orElseThrow(), 1e-9);
    }

    @Test
    void weightReturnsValidationErrorWhenOwnerEmailIsBlank() {
        var result = service.handle(new GetScaleWeightQuery(""));

        assertTrue(result.isFailure());
        var error = ((online.entreprenly.platform.shared.application.result.Result.Failure<?, ?>) result).error();
        assertEquals("VALIDATION_ERROR", ((online.entreprenly.platform.shared.application.result.ApplicationError) error).code());
    }

    @Test
    void weightReturnsValidationErrorWhenOwnerEmailIsNull() {
        var result = service.handle(new GetScaleWeightQuery(null));

        assertTrue(result.isFailure());
    }
}

