package online.entreprenly.platform.sales.application;

import online.entreprenly.platform.sales.application.internal.queryservices.CashRegisterQueryServiceImpl;
import online.entreprenly.platform.sales.domain.model.aggregates.CashRegister;
import online.entreprenly.platform.sales.domain.model.aggregates.Sale;
import online.entreprenly.platform.sales.domain.model.queries.GetCashRegisterByDateQuery;
import online.entreprenly.platform.sales.domain.model.valueobjects.PaymentMethod;
import online.entreprenly.platform.sales.domain.model.valueobjects.SaleItem;
import online.entreprenly.platform.sales.domain.model.valueobjects.SaleStatus;
import online.entreprenly.platform.sales.support.InMemoryCashRegisterRepository;
import online.entreprenly.platform.sales.support.InMemorySaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CashRegisterQueryServiceImplTest {

    private static final String MERCHANT_A = "merchantA@shop.com";
    private static final String MERCHANT_B = "merchantB@shop.com";
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 11);

    private InMemoryCashRegisterRepository cashRegisterRepository;
    private InMemorySaleRepository saleRepository;
    private CashRegisterQueryServiceImpl queryService;

    @BeforeEach
    void setUp() {
        cashRegisterRepository = new InMemoryCashRegisterRepository();
        saleRepository = new InMemorySaleRepository();
        queryService = new CashRegisterQueryServiceImpl(cashRegisterRepository, saleRepository);
    }

    @Test
    void returnsZeroedRegisterWhenNoSalesAndNoRegisterExistYet() {
        var result = queryService.handle(new GetCashRegisterByDateQuery(MERCHANT_A, 1L, TODAY));

        assertNotNull(result);
        assertEquals(0.0, result.getTotal());
        assertEquals(0.0, result.getCashTotal());
        assertEquals(0.0, result.getDigitalTotal());
        assertEquals(0, result.getSalesCount());

        // Verifies it was persisted for subsequent reconsults
        var persisted = cashRegisterRepository.findByOwnerEmailAndDate(MERCHANT_A, TODAY);
        assertNotNull(persisted.orElse(null));
    }

    @Test
    void returnsExistingPersistedRegisterOnReconsult() {
        var register = CashRegister.empty(MERCHANT_A, 1L, TODAY);
        register.recordSale(PaymentMethod.CASH, 100.0);
        register.recordSale(PaymentMethod.YAPE, 50.0);
        cashRegisterRepository.save(register);

        var result = queryService.handle(new GetCashRegisterByDateQuery(MERCHANT_A, 1L, TODAY));

        assertEquals(100.0, result.getCashTotal());
        assertEquals(50.0, result.getYapeTotal());
        assertEquals(50.0, result.getDigitalTotal());
        assertEquals(150.0, result.getTotal());
        assertEquals(2, result.getSalesCount());
    }

    @Test
    void enforcesStrictTenantIsolationBetweenMerchants() {
        // Merchant A has sales totaling S/ 200 (CASH)
        var registerA = CashRegister.empty(MERCHANT_A, 1L, TODAY);
        registerA.recordSale(PaymentMethod.CASH, 200.0);
        cashRegisterRepository.save(registerA);

        // Merchant B has sales totaling S/ 50 (YAPE)
        var registerB = CashRegister.empty(MERCHANT_B, 1L, TODAY);
        registerB.recordSale(PaymentMethod.YAPE, 50.0);
        cashRegisterRepository.save(registerB);

        var queryA = queryService.handle(new GetCashRegisterByDateQuery(MERCHANT_A, TODAY));
        var queryB = queryService.handle(new GetCashRegisterByDateQuery(MERCHANT_B, TODAY));

        // Merchant A only sees their own totals
        assertEquals(200.0, queryA.getCashTotal());
        assertEquals(0.0, queryA.getDigitalTotal());
        assertEquals(200.0, queryA.getTotal());

        // Merchant B only sees their own totals
        assertEquals(0.0, queryB.getCashTotal());
        assertEquals(50.0, queryB.getYapeTotal());
        assertEquals(50.0, queryB.getDigitalTotal());
        assertEquals(50.0, queryB.getTotal());
    }
}
