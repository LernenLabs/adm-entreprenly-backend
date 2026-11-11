package online.entreprenly.platform;

import jakarta.persistence.EntityManager;
import online.entreprenly.platform.inventory.infrastructure.persistence.jpa.entities.UnitProductPersistenceEntity;
import online.entreprenly.platform.inventory.infrastructure.persistence.jpa.repositories.UnitProductPersistenceRepository;
import online.entreprenly.platform.sales.domain.model.valueobjects.PaymentMethod;
import online.entreprenly.platform.sales.domain.model.valueobjects.SaleStatus;
import online.entreprenly.platform.sales.infrastructure.persistence.jpa.embeddables.SaleItemEmbeddable;
import online.entreprenly.platform.sales.infrastructure.persistence.jpa.entities.CashRegisterPersistenceEntity;
import online.entreprenly.platform.sales.infrastructure.persistence.jpa.entities.SalePersistenceEntity;
import online.entreprenly.platform.sales.infrastructure.persistence.jpa.repositories.CashRegisterPersistenceRepository;
import online.entreprenly.platform.sales.infrastructure.persistence.jpa.repositories.SalePersistenceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cross-cutting JPA persistence and multi-tenant isolation integration test suite (US-93 / S2-T26).
 *
 * <p>Consolidates evidence verifying:
 * <ul>
 *   <li>Reconsultation integrity after persistence context restart / clear (flush & clear).</li>
 *   <li>Coherent JPA mappings, embeddables, element collections, and auditing fields.</li>
 *   <li>Strict tenant isolation by merchant ({@code owner_email}): prevention of cross-merchant reads.</li>
 *   <li>Rejection of direct ID access when queried under a foreign merchant's identity.</li>
 * </ul>
 * </p>
 */
@SpringBootTest(properties = {
    "spring.profiles.active=av2",
    "spring.datasource.url=jdbc:h2:mem:jpa_isolation_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class JpaPersistenceAndTenantIsolationTest {

    private static final String MERCHANT_A = "merchant.alpha@shop.com";
    private static final String MERCHANT_B = "merchant.beta@shop.com";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Lima");

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SalePersistenceRepository saleRepository;

    @Autowired
    private CashRegisterPersistenceRepository cashRegisterRepository;

    @Autowired
    private UnitProductPersistenceRepository unitProductRepository;

    @Test
    @DisplayName("S2-T26.1: Reconsulta tras reinicio de sesión JPA preserva integridad de relaciones")
    void reconsultAfterSessionRestartMaintainsEntityIntegrityAndRelations() {
        var today = LocalDate.now(BUSINESS_ZONE);
        var now = Instant.now();

        // 1. Guardar Producto en Inventario
        var product = new UnitProductPersistenceEntity();
        product.setOwnerEmail(MERCHANT_A);
        product.setName("Queso Paria 500g");
        product.setDescription("Queso artesanal andino");
        product.setPrice(18.50);
        product.setWeightGrams(500.0);
        product.setBrand("Lácteos del Sur");
        var savedProduct = unitProductRepository.save(product);

        // 2. Guardar Venta con Líneas (@ElementCollection / sale_items)
        var sale = new SalePersistenceEntity();
        sale.setOwnerEmail(MERCHANT_A);
        sale.setSellerId(1L);
        sale.setPaymentMethod(PaymentMethod.YAPE);
        sale.setStatus(SaleStatus.COMPLETED);
        sale.setSaleCreatedAt(now);
        sale.setCompletedAt(now);
        sale.setTotal(37.00);

        var item = new SaleItemEmbeddable(savedProduct.getId(), savedProduct.getName(), 2, null, 18.50, 37.00);
        sale.getItems().add(item);
        var savedSale = saleRepository.save(sale);

        // 3. Guardar Acumulado de Caja Diario
        var register = new CashRegisterPersistenceEntity();
        register.setOwnerEmail(MERCHANT_A);
        register.setSellerId(1L);
        register.setRegisterDate(today);
        register.setCashTotal(0.0);
        register.setYapeTotal(37.00);
        register.setPlinTotal(0.0);
        register.setCardTotal(0.0);
        register.setDigitalTotal(37.00);
        register.setTotal(37.00);
        register.setSalesCount(1);
        register.setCashSalesCount(0);
        register.setDigitalSalesCount(1);
        var savedRegister = cashRegisterRepository.save(register);

        // 4. Forzar flush y limpiar el contexto de persistencia (emulando desconexión/reinicio de sesión)
        entityManager.flush();
        entityManager.clear();

        // 5. Reconsulta directa desde la base de datos
        var reconsultedSale = saleRepository.findByIdAndOwnerEmail(savedSale.getId(), MERCHANT_A);
        assertTrue(reconsultedSale.isPresent(), "La venta debe persistir tras limpiar el contexto");
        assertEquals(37.00, reconsultedSale.get().getTotal());
        assertEquals(1, reconsultedSale.get().getItems().size(), "La relación @ElementCollection de items debe persistir");
        assertEquals("Queso Paria 500g", reconsultedSale.get().getItems().get(0).getProductName());

        var reconsultedRegister = cashRegisterRepository.findByOwnerEmailAndRegisterDate(MERCHANT_A, today);
        assertTrue(reconsultedRegister.isPresent(), "El arqueo de caja debe persistir tras limpiar el contexto");
        assertEquals(37.00, reconsultedRegister.get().getYapeTotal());
        assertEquals(37.00, reconsultedRegister.get().getDigitalTotal());
        assertEquals(37.00, reconsultedRegister.get().getTotal());
        assertEquals(1, reconsultedRegister.get().getSalesCount());

        var reconsultedProduct = unitProductRepository.findById(savedProduct.getId());
        assertTrue(reconsultedProduct.isPresent());
        assertEquals("Queso Paria 500g", reconsultedProduct.get().getName());
        assertNotNull(reconsultedProduct.get().getCreatedAt(), "Auditoría createdAt debe estar poblada");
    }

    @Test
    @DisplayName("S2-T26.2: Rechazo de lectura y modificación de datos entre comerciantes ajenos")
    void tenantIsolationPreventsReadingOrModifyingForeignData() {
        var today = LocalDate.now(BUSINESS_ZONE);

        // 1. Comerciante A registra sus datos
        var saleA = new SalePersistenceEntity();
        saleA.setOwnerEmail(MERCHANT_A);
        saleA.setSellerId(1L);
        saleA.setPaymentMethod(PaymentMethod.CASH);
        saleA.setStatus(SaleStatus.COMPLETED);
        saleA.setTotal(150.00);
        saleA.setSaleCreatedAt(Instant.now());
        var savedSaleA = saleRepository.save(saleA);

        var registerA = new CashRegisterPersistenceEntity();
        registerA.setOwnerEmail(MERCHANT_A);
        registerA.setSellerId(1L);
        registerA.setRegisterDate(today);
        registerA.setCashTotal(150.00);
        registerA.setTotal(150.00);
        registerA.setSalesCount(1);
        cashRegisterRepository.save(registerA);

        var productA = new UnitProductPersistenceEntity();
        productA.setOwnerEmail(MERCHANT_A);
        productA.setName("Producto Exclusivo Comerciante A");
        productA.setPrice(50.0);
        productA.setWeightGrams(100.0);
        unitProductRepository.save(productA);

        entityManager.flush();
        entityManager.clear();

        // 2. Comerciante B intenta listar ventas del Comerciante A
        var salesMerchantB = saleRepository.findAllByOwnerEmail(MERCHANT_B);
        assertTrue(salesMerchantB.isEmpty(), "Comerciante B no debe ver las ventas de Comerciante A");

        // 3. Comerciante B intenta acceder a la venta del Comerciante A por ID
        var foreignSaleAccess = saleRepository.findByIdAndOwnerEmail(savedSaleA.getId(), MERCHANT_B);
        assertTrue(foreignSaleAccess.isEmpty(), "Acceso directo a venta ajena por ID debe ser rechazado (retornar Optional.empty())");

        // 4. Comerciante B intenta consultar la caja del Comerciante A
        var registerMerchantB = cashRegisterRepository.findByOwnerEmailAndRegisterDate(MERCHANT_B, today);
        assertTrue(registerMerchantB.isEmpty(), "Comerciante B no debe acceder a la caja de Comerciante A");

        // 5. Comerciante B intenta listar productos del Comerciante A
        var productsMerchantB = unitProductRepository.findAllByOwnerEmail(MERCHANT_B);
        assertTrue(productsMerchantB.isEmpty(), "Comerciante B no debe ver el catálogo de Comerciante A");
    }

    @Test
    @DisplayName("S2-T26.2: Coexistencia simultánea y aislamiento de datos entre comerciantes")
    void simultaneousMerchantDataCoexistenceRemainsStrictlyIsolated() {
        var today = LocalDate.now(BUSINESS_ZONE);

        // Merchant A registra venta de S/ 80.00 en efectivo
        var saleA = new SalePersistenceEntity();
        saleA.setOwnerEmail(MERCHANT_A);
        saleA.setSellerId(1L);
        saleA.setPaymentMethod(PaymentMethod.CASH);
        saleA.setStatus(SaleStatus.COMPLETED);
        saleA.setTotal(80.00);
        saleA.setSaleCreatedAt(Instant.now());
        saleRepository.save(saleA);

        // Merchant B registra venta de S/ 120.00 vía Yape
        var saleB = new SalePersistenceEntity();
        saleB.setOwnerEmail(MERCHANT_B);
        saleB.setSellerId(2L);
        saleB.setPaymentMethod(PaymentMethod.YAPE);
        saleB.setStatus(SaleStatus.COMPLETED);
        saleB.setTotal(120.00);
        saleB.setSaleCreatedAt(Instant.now());
        saleRepository.save(saleB);

        entityManager.flush();
        entityManager.clear();

        var listA = saleRepository.findAllByOwnerEmail(MERCHANT_A);
        var listB = saleRepository.findAllByOwnerEmail(MERCHANT_B);

        assertEquals(1, listA.size());
        assertEquals(80.00, listA.get(0).getTotal());
        assertEquals(PaymentMethod.CASH, listA.get(0).getPaymentMethod());

        assertEquals(1, listB.size());
        assertEquals(120.00, listB.get(0).getTotal());
        assertEquals(PaymentMethod.YAPE, listB.get(0).getPaymentMethod());
    }
}

