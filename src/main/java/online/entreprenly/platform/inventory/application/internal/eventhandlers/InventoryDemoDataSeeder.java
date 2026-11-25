package online.entreprenly.platform.inventory.application.internal.eventhandlers;

import online.entreprenly.platform.inventory.application.commandservices.UnitLotCommandService;
import online.entreprenly.platform.inventory.application.commandservices.UnitProductCommandService;
import online.entreprenly.platform.inventory.application.commandservices.WeightLotCommandService;
import online.entreprenly.platform.inventory.application.commandservices.WeightProductCommandService;
import online.entreprenly.platform.inventory.domain.model.aggregates.UnitProduct;
import online.entreprenly.platform.inventory.domain.model.aggregates.WeightProduct;
import online.entreprenly.platform.inventory.domain.model.commands.CreateUnitLotCommand;
import online.entreprenly.platform.inventory.domain.model.commands.CreateUnitProductCommand;
import online.entreprenly.platform.inventory.domain.model.commands.CreateWeightLotCommand;
import online.entreprenly.platform.inventory.domain.model.commands.CreateWeightProductCommand;
import online.entreprenly.platform.inventory.domain.model.commands.UpdateUnitProductCommand;
import online.entreprenly.platform.inventory.domain.model.commands.UpdateWeightProductCommand;
import online.entreprenly.platform.inventory.domain.repositories.UnitProductRepository;
import online.entreprenly.platform.inventory.domain.repositories.WeightProductRepository;
import online.entreprenly.platform.shared.application.result.ApplicationError;
import online.entreprenly.platform.shared.application.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Seeds the demo store catalog: 20 believable minimarket products (14 sold by unit, 6 by weight),
 * each with its own photo, description and initial lot, plus one extra product reserved for
 * demonstrating deletion.
 *
 * <p>Idempotent: a product is created only when the demo account has no product with its QR code
 * or name. Existing products keep their name, price and stock; only a missing photo, description,
 * brand or QR code is filled in. A deleted seed product, such as the deletion demo, is created
 * again on the next start so the demo can be repeated.</p>
 */
@Service
@Slf4j
public class InventoryDemoDataSeeder {

    private static final String DEMO_OWNER_EMAIL = "test.beta@entreprenly.online";

    private static final String OFF = "https://images.openfoodfacts.org/images/products/";
    private static final String COMMONS = "https://upload.wikimedia.org/wikipedia/commons/thumb/";

    /** Short placeholder descriptions of the first demo version, replaced by the new ones. */
    private static final Set<String> OLD_DESCRIPTIONS = Set.of("Gaseosa en botella", "Galletas rellenas",
            "Leche evaporada en lata", "Manzana fresca vendida por kilo", "Platano fresco por kilo",
            "Tomate fresco por kilo");

    /** Product sold per unit, with the quantity and shelf life of its first lot (0 units = no lot). */
    private record UnitSeed(String codeQR, String name, String description, double price, double weightGrams,
                            String brand, String imageUrl, String lotCodeQR, int quantity, int expiresInDays) {
    }

    /** Product sold by weight, with the kilograms of its first lot. */
    private record WeightSeed(String codeQR, String name, String description, double pricePerKg, String imageUrl,
                              String lotCodeQR, double quantityKg) {
    }

    private static final List<UnitSeed> UNIT_SEEDS = List.of(
            new UnitSeed("QR-UP-0001", "Coca-Cola 500ml",
                    "Gaseosa Coca-Cola sabor original en botella personal de 500 ml. Servir bien helada.",
                    3.5, 500, "Coca-Cola",
                    OFF + "544/900/000/0996/front_en.1129.400.jpg", "QR-UL-0001", 24, 180),
            new UnitSeed("QR-UP-0002", "Galletas Oreo",
                    "Galletas de chocolate rellenas de crema sabor vainilla. Paquete de 6 unidades.",
                    2.5, 110, "Oreo",
                    OFF + "759/001/125/1100/front_es.72.400.jpg", "QR-UL-0002", 40, 120),
            new UnitSeed("QR-UP-0003", "Leche Gloria Entera",
                    "Leche evaporada entera en lata de 400 g, ideal para el desayuno y la repostería.",
                    4.2, 400, "Gloria",
                    OFF + "775/127/103/4081/front_en.25.400.jpg", "QR-UL-0003", 30, 240),
            new UnitSeed("QR-UP-0004", "Inca Kola 500ml",
                    "La bebida de sabor nacional: gaseosa dorada en botella de 500 ml.",
                    3.5, 500, "Inca Kola",
                    OFF + "775/023/617/3896/front_pt.24.400.jpg", "QR-UL-0004", 36, 180),
            new UnitSeed("QR-UP-0005", "Agua San Luis sin gas 625ml",
                    "Agua de mesa sin gas en botella de 625 ml, para llevar en la mochila o la lonchera.",
                    1.8, 625, "San Luis",
                    OFF + "775/018/200/7269/front_en.20.400.jpg", "QR-UL-0005", 48, 365),
            new UnitSeed("QR-UP-0006", "Spaghetti Don Vittorio 450g",
                    "Fideo spaghetti de sémola de trigo durum en bolsa de 450 g. Cocción en 9 minutos.",
                    3.9, 450, "Don Vittorio",
                    OFF + "775/024/328/6145/front_es.39.400.jpg", "QR-UL-0006", 25, 300),
            new UnitSeed("QR-UP-0007", "Atún en aceite 170g",
                    "Trozos de atún en aceite vegetal, lata de 170 g. Listo para ensaladas y sánguches.",
                    6.5, 170, "Florida",
                    OFF + "075/155/100/0000/front_es.3.400.jpg", "QR-UL-0007", 3, 540),
            new UnitSeed("QR-UP-0008", "Chocolate Sublime clásico",
                    "Chocolate con leche y maní en barra de 30 g. El antojo de siempre para el recreo.",
                    2.0, 30, "Sublime",
                    OFF + "761/303/652/4667/front_es.14.400.jpg", "QR-UL-0008", 60, 200),
            new UnitSeed("QR-UP-0009", "Papas Lay's clásicas",
                    "Papas fritas en hojuelas con un toque de sal, bolsa de 42 g.",
                    2.5, 42, "Lay's",
                    OFF + "775/202/500/6880/front_es.13.400.jpg", "QR-UL-0009", 30, 90),
            new UnitSeed("QR-UP-0010", "Yogurt Gloria vainilla 1kg",
                    "Yogurt bebible sabor vainilla en botella de 1 kg. Mantener refrigerado.",
                    7.9, 1000, "Gloria",
                    OFF + "775/127/103/0427/front_en.3.400.jpg", "QR-UL-0010", 12, 3),
            new UnitSeed("QR-UP-0011", "Avena Quaker instantánea",
                    "Hojuelas de avena instantánea en bolsa de 300 g, listas en 3 minutos.",
                    4.5, 300, "Quaker",
                    OFF + "775/857/400/2373/front_fr.3.400.jpg", "QR-UL-0011", 0, 0),
            new UnitSeed("QR-UP-0012", "Sal de mesa Emsal 1kg",
                    "Sal marina yodada y fluorada en bolsa de 1 kg para la cocina diaria.",
                    1.6, 1000, "Emsal",
                    OFF + "775/501/900/0147/front_es.22.400.jpg", "QR-UL-0012", 20, 720),
            new UnitSeed("QR-UP-0013", "Galleta Cua Cua",
                    "Galleta bañada en chocolate con relleno de crema, unidad de 28 g.",
                    1.0, 28, "Cua Cua",
                    OFF + "775/016/800/1151/front_en.7.400.jpg", "QR-UL-0013", 50, 150),
            new UnitSeed("QR-UP-0014", "Café Altomayo liofilizado",
                    "Café instantáneo liofilizado 100 % peruano, frasco de 50 g.",
                    9.9, 50, "Altomayo",
                    OFF + "775/046/300/1412/front_es.4.400.jpg", "QR-UL-0014", 10, 400)
    );

    private static final List<WeightSeed> WEIGHT_SEEDS = List.of(
            new WeightSeed("QR-WP-0001", "Manzana Roja",
                    "Manzana roja crocante y dulce, seleccionada a mano. Precio por kilo.", 4.5,
                    COMMONS + "1/15/Red_Apple.jpg/500px-Red_Apple.jpg", "QR-WL-0001", 20),
            new WeightSeed("QR-WP-0002", "Platano de Seda",
                    "Plátano de seda maduro, perfecto para el desayuno o un batido. Precio por kilo.", 3.0,
                    COMMONS + "8/8a/Banana-Single.jpg/500px-Banana-Single.jpg", "QR-WL-0002", 35),
            new WeightSeed("QR-WP-0003", "Tomate",
                    "Tomate italiano fresco y firme para ensaladas y aderezos. Precio por kilo.", 3.8,
                    COMMONS + "8/89/Tomato_je.jpg/500px-Tomato_je.jpg", "QR-WL-0003", 15),
            new WeightSeed("QR-WP-0004", "Papa amarilla",
                    "Papa amarilla harinosa, la ideal para la causa y el puré. Precio por kilo.", 5.2,
                    COMMONS + "a/ab/Patates.jpg/500px-Patates.jpg", "QR-WL-0004", 40),
            new WeightSeed("QR-WP-0005", "Cebolla roja",
                    "Cebolla roja firme y de sabor intenso para el lomo saltado. Precio por kilo.", 2.9,
                    COMMONS + "2/25/Onion_on_White.JPG/500px-Onion_on_White.JPG", "QR-WL-0005", 25),
            new WeightSeed("QR-WP-0006", "Limón",
                    "Limón sutil jugoso, el infaltable del ceviche. Precio por kilo.", 6.0,
                    COMMONS + "6/68/Lime-Whole-Split.jpg/500px-Lime-Whole-Split.jpg", "QR-WL-0006", 4)
    );

    /** Kept apart from the 20 catalog products: delete it to demonstrate deletion. */
    private static final WeightSeed DELETE_DEMO_SEED = new WeightSeed("QR-DEMO-BORRAR", "Palta Hass (demo borrado)",
            "Producto aparte para demostrar el borrado: elimínalo junto con su lote.", 9.5,
            COMMONS + "c/c9/Avocado_Hass_-_single_and_halved.jpg/500px-Avocado_Hass_-_single_and_halved.jpg",
            "QR-DEMO-BORRAR-L1", 6);

    private final UnitProductCommandService unitProductCommandService;
    private final WeightProductCommandService weightProductCommandService;
    private final UnitLotCommandService unitLotCommandService;
    private final WeightLotCommandService weightLotCommandService;
    private final UnitProductRepository unitProductRepository;
    private final WeightProductRepository weightProductRepository;

    public InventoryDemoDataSeeder(UnitProductCommandService unitProductCommandService,
                                   WeightProductCommandService weightProductCommandService,
                                   UnitLotCommandService unitLotCommandService,
                                   WeightLotCommandService weightLotCommandService,
                                   UnitProductRepository unitProductRepository,
                                   WeightProductRepository weightProductRepository) {
        this.unitProductCommandService = unitProductCommandService;
        this.weightProductCommandService = weightProductCommandService;
        this.unitLotCommandService = unitLotCommandService;
        this.weightLotCommandService = weightLotCommandService;
        this.unitProductRepository = unitProductRepository;
        this.weightProductRepository = weightProductRepository;
    }

    @EventListener
    public void on(ApplicationReadyEvent event) {
        log.info("[inventory-seeder] ensuring demo catalog for {}", DEMO_OWNER_EMAIL);
        var now = Instant.now();
        var unitProducts = unitProductRepository.findAllByOwnerEmail(DEMO_OWNER_EMAIL);
        var weightProducts = weightProductRepository.findAllByOwnerEmail(DEMO_OWNER_EMAIL);

        int created = 0;
        for (var seed : UNIT_SEEDS) {
            var existing = unitProducts.stream()
                    .filter(p -> seed.codeQR().equals(p.getCodeQR()) || seed.name().equalsIgnoreCase(p.getName()))
                    .findFirst();
            if (existing.isPresent()) {
                completeUnitProduct(existing.get(), seed);
            } else if (createUnitProduct(seed, now)) {
                created++;
            }
        }
        var weightSeeds = new ArrayList<>(WEIGHT_SEEDS);
        weightSeeds.add(DELETE_DEMO_SEED);
        for (var seed : weightSeeds) {
            var existing = weightProducts.stream()
                    .filter(p -> seed.codeQR().equals(p.getCodeQR()) || seed.name().equalsIgnoreCase(p.getName()))
                    .findFirst();
            if (existing.isPresent()) {
                completeWeightProduct(existing.get(), seed);
            } else if (createWeightProduct(seed, now)) {
                created++;
            }
        }
        log.info("[inventory-seeder] demo catalog ready for {} ({} product(s) created)", DEMO_OWNER_EMAIL, created);
    }

    private boolean createUnitProduct(UnitSeed seed, Instant now) {
        var result = unitProductCommandService.handle(new CreateUnitProductCommand(DEMO_OWNER_EMAIL, seed.name(),
                seed.description(), seed.codeQR(), seed.price(), seed.weightGrams(), seed.brand(), seed.imageUrl()));
        var product = success(result);
        if (product.isEmpty()) {
            log.warn("[inventory-seeder] could not seed unit product '{}': {}", seed.name(), result);
            return false;
        }
        // A seed with no units is left without lots so the catalog shows it as out of stock.
        if (seed.quantity() > 0) {
            unitLotCommandService.handle(new CreateUnitLotCommand(DEMO_OWNER_EMAIL, product.get().getId(),
                    seed.lotCodeQR(), now.minus(2, ChronoUnit.DAYS), seed.quantity(),
                    now.plus(seed.expiresInDays(), ChronoUnit.DAYS)));
        }
        return true;
    }

    private boolean createWeightProduct(WeightSeed seed, Instant now) {
        var result = weightProductCommandService.handle(new CreateWeightProductCommand(DEMO_OWNER_EMAIL,
                seed.name(), seed.description(), seed.codeQR(), seed.pricePerKg(), seed.imageUrl()));
        var product = success(result);
        if (product.isEmpty()) {
            log.warn("[inventory-seeder] could not seed weight product '{}': {}", seed.name(), result);
            return false;
        }
        weightLotCommandService.handle(new CreateWeightLotCommand(DEMO_OWNER_EMAIL, product.get().getId(),
                seed.lotCodeQR(), now.minus(1, ChronoUnit.DAYS), seed.quantityKg()));
        return true;
    }

    /** Fills a missing photo, description, brand or QR code without touching name, price or stock. */
    private void completeUnitProduct(UnitProduct product, UnitSeed seed) {
        if (isComplete(product.getImageUrl(), product.getDescription(), product.getCodeQR())) return;
        unitProductCommandService.handle(new UpdateUnitProductCommand(DEMO_OWNER_EMAIL, product.getId(),
                product.getName(),
                description(product.getDescription(), seed.description()),
                orElse(product.getCodeQR(), seed.codeQR()),
                product.getPrice(),
                product.getWeightGrams() > 0 ? product.getWeightGrams() : seed.weightGrams(),
                orElse(product.getBrand(), seed.brand()),
                orElse(product.getImageUrl(), seed.imageUrl())));
    }

    private void completeWeightProduct(WeightProduct product, WeightSeed seed) {
        if (isComplete(product.getImageUrl(), product.getDescription(), product.getCodeQR())) return;
        weightProductCommandService.handle(new UpdateWeightProductCommand(DEMO_OWNER_EMAIL, product.getId(),
                product.getName(),
                description(product.getDescription(), seed.description()),
                orElse(product.getCodeQR(), seed.codeQR()),
                product.getPricePerKg(),
                orElse(product.getImageUrl(), seed.imageUrl())));
    }

    private static boolean isComplete(String imageUrl, String description, String codeQR) {
        return !isBlank(imageUrl) && !isBlank(codeQR) && !isBlank(description)
                && !OLD_DESCRIPTIONS.contains(description);
    }

    private static String description(String current, String seed) {
        return isBlank(current) || OLD_DESCRIPTIONS.contains(current) ? seed : current;
    }

    private static String orElse(String current, String fallback) {
        return isBlank(current) ? fallback : current;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static <T> Optional<T> success(Result<T, ApplicationError> result) {
        return result instanceof Result.Success<T, ApplicationError> s ? Optional.of(s.value()) : Optional.empty();
    }
}
