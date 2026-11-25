package online.entreprenly.platform.inventory.application.internal.queryservices;

import online.entreprenly.platform.inventory.application.queryservices.PublicCatalogQueryService;
import online.entreprenly.platform.inventory.domain.model.aggregates.UnitLot;
import online.entreprenly.platform.inventory.domain.model.aggregates.WeightLot;
import online.entreprenly.platform.inventory.domain.model.entities.CatalogProduct;
import online.entreprenly.platform.inventory.domain.model.queries.GetPublicCatalogQuery;
import online.entreprenly.platform.inventory.domain.model.valueobjects.ProductType;
import online.entreprenly.platform.inventory.domain.repositories.UnitLotRepository;
import online.entreprenly.platform.inventory.domain.repositories.UnitProductRepository;
import online.entreprenly.platform.inventory.domain.repositories.WeightLotRepository;
import online.entreprenly.platform.inventory.domain.repositories.WeightProductRepository;
import org.springframework.stereotype.Service;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Builds the storefront catalog: every unit and weight product of a store with the stock summed
 * over its lots.
 */
@Service
public class PublicCatalogQueryServiceImpl implements PublicCatalogQueryService {

    private final UnitProductRepository unitProductRepository;
    private final WeightProductRepository weightProductRepository;
    private final UnitLotRepository unitLotRepository;
    private final WeightLotRepository weightLotRepository;

    public PublicCatalogQueryServiceImpl(UnitProductRepository unitProductRepository,
                                         WeightProductRepository weightProductRepository,
                                         UnitLotRepository unitLotRepository,
                                         WeightLotRepository weightLotRepository) {
        this.unitProductRepository = unitProductRepository;
        this.weightProductRepository = weightProductRepository;
        this.unitLotRepository = unitLotRepository;
        this.weightLotRepository = weightLotRepository;
    }

    @Override
    public List<CatalogProduct> handle(GetPublicCatalogQuery query) {
        var owner = query.ownerEmail();
        if (owner == null || owner.isBlank()) {
            return List.of();
        }
        var catalog = new ArrayList<CatalogProduct>();

        var unitStock = unitLotRepository.findAllByOwnerEmail(owner).stream()
                .collect(Collectors.groupingBy(UnitLot::getProductId, Collectors.summingDouble(UnitLot::getQuantity)));
        for (var product : unitProductRepository.findAllByOwnerEmail(owner)) {
            catalog.add(new CatalogProduct(product.getId(), ProductType.UNIT, product.getName(),
                    product.getDescription(), product.getBrand(), product.getImageUrl(), product.getPrice(),
                    unitStock.getOrDefault(product.getId(), 0.0)));
        }

        var weightStock = weightLotRepository.findAllByOwnerEmail(owner).stream()
                .collect(Collectors.groupingBy(WeightLot::getProductId,
                        Collectors.summingDouble(WeightLot::getQuantityKg)));
        for (var product : weightProductRepository.findAllByOwnerEmail(owner)) {
            catalog.add(new CatalogProduct(product.getId(), ProductType.WEIGHT, product.getName(),
                    product.getDescription(), null, product.getImageUrl(), product.getPricePerKg(),
                    weightStock.getOrDefault(product.getId(), 0.0)));
        }

        var collator = Collator.getInstance(Locale.forLanguageTag("es"));
        collator.setStrength(Collator.PRIMARY);
        catalog.sort(Comparator.comparing(CatalogProduct::name, Comparator.nullsLast(collator)));
        return catalog;
    }
}
