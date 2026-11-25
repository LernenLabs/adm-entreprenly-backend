package online.entreprenly.platform.inventory.interfaces.rest;

import online.entreprenly.platform.inventory.application.internal.outboundservices.ExternalProductCatalog;
import online.entreprenly.platform.inventory.application.internal.outboundservices.ExternalProductCatalog.ExternalCatalogUnavailableException;
import online.entreprenly.platform.inventory.application.internal.outboundservices.ExternalProductCatalog.ExternalProductInfo;
import online.entreprenly.platform.inventory.interfaces.rest.resources.BarcodeLookupResource;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class BarcodeLookupsControllerTests {

    private static final ExternalProductInfo INCA_KOLA = new ExternalProductInfo("7750236173896",
            "Inca Kola 500 ml", "Inca Kola", "Gaseosa", "https://example.com/inca.jpg");

    private final ExternalProductCatalog catalog = barcode -> switch (barcode) {
        case "7750236173896" -> Optional.of(INCA_KOLA);
        case "0000000000000" -> throw new ExternalCatalogUnavailableException("down", null);
        default -> Optional.empty();
    };

    private final BarcodeLookupsController controller = new BarcodeLookupsController(catalog);

    @Test
    void knownBarcode_returnsSuggestedProductData() {
        var response = controller.lookup("7750236173896");

        assertEquals(200, response.getStatusCode().value());
        var body = assertInstanceOf(BarcodeLookupResource.class, response.getBody());
        assertEquals("Inca Kola 500 ml", body.name());
        assertEquals("Inca Kola", body.brand());
        assertEquals("7750236173896", body.barcode());
    }

    @Test
    void unknownBarcode_returnsNotFound() {
        assertEquals(404, controller.lookup("7751234567890").getStatusCode().value());
    }

    @Test
    void textThatIsNotABarcode_isRejectedWithoutCallingTheCatalog() {
        assertEquals(400, controller.lookup("QR-UP-0004").getStatusCode().value());
        assertEquals(400, controller.lookup("12345").getStatusCode().value());
    }

    @Test
    void unavailableDatabase_returnsServiceUnavailable() {
        assertEquals(503, controller.lookup("0000000000000").getStatusCode().value());
    }
}
