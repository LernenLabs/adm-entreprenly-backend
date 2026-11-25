package online.entreprenly.platform.inventory.application.internal.outboundservices;

import java.util.Optional;

/**
 * Outbound port to a public product database, used to pre-fill a new product from the barcode
 * printed on its package.
 */
public interface ExternalProductCatalog {

    /**
     * Looks up a packaged product by its barcode.
     *
     * @param barcode an EAN-8, EAN-13, UPC-A or UPC-E barcode (digits only)
     * @return the product data, or empty when the database does not know the barcode
     * @throws ExternalCatalogUnavailableException when the database cannot be reached
     */
    Optional<ExternalProductInfo> findByBarcode(String barcode);

    /**
     * Product data published for a barcode. Any field may be null when the database lacks it.
     *
     * @param barcode     the looked-up barcode
     * @param name        the product name, including its size when known (e.g. "Inca Kola 500 ml")
     * @param brand       the main brand
     * @param description a short description
     * @param imageUrl    the public URL of the front photo
     */
    record ExternalProductInfo(String barcode, String name, String brand, String description, String imageUrl) {
    }

    /** The product database did not answer in time or answered with an error. */
    class ExternalCatalogUnavailableException extends RuntimeException {
        public ExternalCatalogUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
