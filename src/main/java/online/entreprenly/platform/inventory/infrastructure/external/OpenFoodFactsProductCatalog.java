package online.entreprenly.platform.inventory.infrastructure.external;

import online.entreprenly.platform.inventory.application.internal.outboundservices.ExternalProductCatalog;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Optional;

/**
 * {@link ExternalProductCatalog} backed by the public Open Food Facts API, which covers many
 * packaged products sold in Peru. Only the barcode is sent; no account or seller data leaves
 * the platform.
 */
@Service
public class OpenFoodFactsProductCatalog implements ExternalProductCatalog {

    private static final Logger log = LoggerFactory.getLogger(OpenFoodFactsProductCatalog.class);
    private static final String FIELDS = "product_name,product_name_es,generic_name,generic_name_es,brands,quantity,"
            + "image_front_url";

    private final RestClient restClient;

    public OpenFoodFactsProductCatalog(
            RestClient.Builder restClientBuilder,
            @Value("${inventory.barcode-lookup.base-url:https://world.openfoodfacts.org}") String baseUrl) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(8));
        this.restClient = restClientBuilder.clone()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                // Open Food Facts asks API clients to identify themselves.
                .defaultHeader("User-Agent", "EntreprenlyPlatform/1.0 (inventory barcode lookup)")
                .build();
    }

    @Override
    public Optional<ExternalProductInfo> findByBarcode(String barcode) {
        OffResponse response;
        try {
            response = restClient.get()
                    .uri("/api/v2/product/{barcode}?fields={fields}", barcode, FIELDS)
                    .retrieve()
                    .body(OffResponse.class);
        } catch (HttpClientErrorException.NotFound notFound) {
            return Optional.empty();
        } catch (RestClientException ex) {
            log.warn("[inventory] barcode lookup for {} failed: {}", barcode, ex.getMessage());
            throw new ExternalCatalogUnavailableException("The product database is not available right now", ex);
        }
        if (response == null || response.status() != 1 || response.product() == null) {
            return Optional.empty();
        }
        var product = response.product();
        var name = firstText(product.nameEs(), product.name());
        if (name == null) return Optional.empty();
        var quantity = text(product.quantity());
        if (quantity != null && !name.toLowerCase().contains(quantity.toLowerCase())) {
            name = name + " " + quantity;
        }
        var brands = text(product.brands());
        var brand = brands == null ? null : brands.split(",")[0].trim();
        return Optional.of(new ExternalProductInfo(barcode, name, brand,
                firstText(product.genericNameEs(), product.genericName()), text(product.imageFrontUrl())));
    }

    private static String firstText(String preferred, String fallback) {
        var first = text(preferred);
        return first != null ? first : text(fallback);
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OffResponse(int status, OffProduct product) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OffProduct(
            @JsonProperty("product_name") String name,
            @JsonProperty("product_name_es") String nameEs,
            @JsonProperty("generic_name") String genericName,
            @JsonProperty("generic_name_es") String genericNameEs,
            @JsonProperty("brands") String brands,
            @JsonProperty("quantity") String quantity,
            @JsonProperty("image_front_url") String imageFrontUrl) {
    }
}
