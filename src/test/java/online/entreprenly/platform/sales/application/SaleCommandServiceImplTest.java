package online.entreprenly.platform.sales.application;

import online.entreprenly.platform.sales.application.internal.commandservices.SaleCommandServiceImpl;
import online.entreprenly.platform.sales.domain.model.commands.CreateSaleCommand;
import online.entreprenly.platform.sales.domain.model.valueobjects.PaymentMethod;
import online.entreprenly.platform.sales.domain.model.valueobjects.SaleItem;
import online.entreprenly.platform.sales.domain.model.valueobjects.SaleStatus;
import online.entreprenly.platform.sales.support.InMemoryCashRegisterRepository;
import online.entreprenly.platform.sales.support.InMemorySaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaleCommandServiceImplTest {

    private static final String MERCHANT_A = "merchantA@shop.com";
    private static final String MERCHANT_B = "merchantB@shop.com";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Lima");

    private InMemorySaleRepository saleRepository;
    private InMemoryCashRegisterRepository cashRegisterRepository;
    private SaleCommandServiceImpl commandService;

    @BeforeEach
    void setUp() {
        saleRepository = new InMemorySaleRepository();
        cashRegisterRepository = new InMemoryCashRegisterRepository();
        commandService = new SaleCommandServiceImpl(saleRepository, cashRegisterRepository);
    }

    private static CreateSaleCommand createCommand(String owner, PaymentMethod method, double price, int qty) {
        var item = SaleItem.of(1L, "Product", qty, null, price);
        return new CreateSaleCommand(owner, 1L, List.of(item), method, null, SaleStatus.COMPLETED);
    }

    @Test
    void updatesCashRegisterUponRegisteringCashSale() {
        var command = createCommand(MERCHANT_A, PaymentMethod.CASH, 25.50, 2); // 51.00
        var result = commandService.handle(command);

        assertTrue(result.isSuccess());
        var today = LocalDate.now(BUSINESS_ZONE);

        var register = cashRegisterRepository.findByOwnerEmailAndDate(MERCHANT_A, today).orElseThrow();
        assertEquals(51.00, register.getCashTotal());
        assertEquals(0.00, register.getDigitalTotal());
        assertEquals(51.00, register.getTotal());
        assertEquals(1, register.getSalesCount());
        assertEquals(1, register.getCashSalesCount());
    }

    @Test
    void accumulatesCashAndDigitalSalesAcrossChannelsAndMaintainsTotals() {
        var today = LocalDate.now(BUSINESS_ZONE);

        // 1. First sale: Cash S/ 30.00
        commandService.handle(createCommand(MERCHANT_A, PaymentMethod.CASH, 30.00, 1));
        // 2. Second sale: Yape S/ 45.00
        commandService.handle(createCommand(MERCHANT_A, PaymentMethod.YAPE, 45.00, 1));
        // 3. Third sale: Plin S/ 20.00
        commandService.handle(createCommand(MERCHANT_A, PaymentMethod.PLIN, 20.00, 1));
        // 4. Fourth sale: Card S/ 50.00
        commandService.handle(createCommand(MERCHANT_A, PaymentMethod.CARD, 50.00, 1));

        var register = cashRegisterRepository.findByOwnerEmailAndDate(MERCHANT_A, today).orElseThrow();

        // Check channel breakdown
        assertEquals(30.00, register.getCashTotal());
        assertEquals(45.00, register.getYapeTotal());
        assertEquals(20.00, register.getPlinTotal());
        assertEquals(50.00, register.getCardTotal());

        // Check digital aggregate = 45 + 20 + 50 = 115.00
        assertEquals(115.00, register.getDigitalTotal());

        // Check grand total = 30 + 115 = 145.00
        assertEquals(145.00, register.getTotal());
        assertEquals(4, register.getSalesCount());
        assertEquals(1, register.getCashSalesCount());
        assertEquals(3, register.getDigitalSalesCount());
    }

    @Test
    void isolatesCashRegistersBetweenMerchants() {
        var today = LocalDate.now(BUSINESS_ZONE);

        commandService.handle(createCommand(MERCHANT_A, PaymentMethod.CASH, 100.00, 1));
        commandService.handle(createCommand(MERCHANT_B, PaymentMethod.YAPE, 75.00, 1));

        var registerA = cashRegisterRepository.findByOwnerEmailAndDate(MERCHANT_A, today).orElseThrow();
        var registerB = cashRegisterRepository.findByOwnerEmailAndDate(MERCHANT_B, today).orElseThrow();

        assertEquals(100.00, registerA.getCashTotal());
        assertEquals(0.00, registerA.getDigitalTotal());
        assertEquals(100.00, registerA.getTotal());

        assertEquals(0.00, registerB.getCashTotal());
        assertEquals(75.00, registerB.getDigitalTotal());
        assertEquals(75.00, registerB.getTotal());
    }
}
