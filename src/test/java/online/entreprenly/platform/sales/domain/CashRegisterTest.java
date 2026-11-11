package online.entreprenly.platform.sales.domain;

import online.entreprenly.platform.sales.domain.model.aggregates.CashRegister;
import online.entreprenly.platform.sales.domain.model.aggregates.Sale;
import online.entreprenly.platform.sales.domain.model.valueobjects.PaymentMethod;
import online.entreprenly.platform.sales.domain.model.valueobjects.SaleItem;
import online.entreprenly.platform.sales.domain.model.valueobjects.SaleStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CashRegisterTest {

    private static final String OWNER = "owner@shop.com";
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 11);

    @Test
    void emptyRegisterHasZeroTotals() {
        var register = CashRegister.empty(OWNER, 1L, TODAY);

        assertEquals(0.0, register.getCashTotal());
        assertEquals(0.0, register.getYapeTotal());
        assertEquals(0.0, register.getPlinTotal());
        assertEquals(0.0, register.getCardTotal());
        assertEquals(0.0, register.getDigitalTotal());
        assertEquals(0.0, register.getTotal());
        assertEquals(0, register.getSalesCount());
        assertEquals(0, register.getCashSalesCount());
        assertEquals(0, register.getDigitalSalesCount());
    }

    @Test
    void recordsCashSaleProperly() {
        var register = CashRegister.empty(OWNER, 1L, TODAY);
        register.recordSale(PaymentMethod.CASH, 45.50);

        assertEquals(45.50, register.getCashTotal());
        assertEquals(0.0, register.getDigitalTotal());
        assertEquals(45.50, register.getTotal());
        assertEquals(1, register.getSalesCount());
        assertEquals(1, register.getCashSalesCount());
        assertEquals(0, register.getDigitalSalesCount());
    }

    @Test
    void recordsDigitalSalesByChannelAndAggregatesProperly() {
        var register = CashRegister.empty(OWNER, 1L, TODAY);

        register.recordSale(PaymentMethod.YAPE, 50.00);
        register.recordSale(PaymentMethod.PLIN, 25.50);
        register.recordSale(PaymentMethod.CARD, 100.25);

        assertEquals(0.0, register.getCashTotal());
        assertEquals(50.00, register.getYapeTotal());
        assertEquals(25.50, register.getPlinTotal());
        assertEquals(100.25, register.getCardTotal());

        // Digital aggregate: 50.00 + 25.50 + 100.25 = 175.75
        assertEquals(175.75, register.getDigitalTotal());
        assertEquals(175.75, register.getTotal());
        assertEquals(3, register.getSalesCount());
        assertEquals(0, register.getCashSalesCount());
        assertEquals(3, register.getDigitalSalesCount());
    }

    @Test
    void contrastsCashAndDigitalChannelsAccurately() {
        var register = CashRegister.empty(OWNER, 1L, TODAY);

        register.recordSale(PaymentMethod.CASH, 120.00);
        register.recordSale(PaymentMethod.YAPE, 80.00);
        register.recordSale(PaymentMethod.CASH, 30.50);
        register.recordSale(PaymentMethod.CARD, 50.00);

        // Cash: 120.00 + 30.50 = 150.50
        assertEquals(150.50, register.getCashTotal());
        assertEquals(2, register.getCashSalesCount());

        // Digital: 80.00 (yape) + 50.00 (card) = 130.00
        assertEquals(80.00, register.getYapeTotal());
        assertEquals(50.00, register.getCardTotal());
        assertEquals(130.00, register.getDigitalTotal());
        assertEquals(2, register.getDigitalSalesCount());

        // Grand total = Cash + Digital = 280.50
        assertEquals(280.50, register.getTotal());
        assertEquals(4, register.getSalesCount());
        assertEquals(register.getCashTotal() + register.getDigitalTotal(), register.getTotal(), 1e-9);
    }

    @Test
    void buildsFromExistingSalesIgnoringCancelledSales() {
        var item1 = SaleItem.of(1L, "Apples", null, 2.5, 10.0); // 25.0
        var item2 = SaleItem.of(2L, "Milk", 2, null, 5.0);       // 10.0

        var saleCash = new Sale(OWNER, 1L, List.of(item1), PaymentMethod.CASH, null, SaleStatus.COMPLETED);
        var saleYape = new Sale(OWNER, 1L, List.of(item2), PaymentMethod.YAPE, null, SaleStatus.COMPLETED);
        var saleCancelled = new Sale(OWNER, 1L, List.of(item1), PaymentMethod.CARD, null, SaleStatus.CANCELLED);

        var register = CashRegister.fromSales(OWNER, 1L, TODAY, List.of(saleCash, saleYape, saleCancelled));

        assertEquals(25.0, register.getCashTotal());
        assertEquals(10.0, register.getYapeTotal());
        assertEquals(0.0, register.getCardTotal());
        assertEquals(10.0, register.getDigitalTotal());
        assertEquals(35.0, register.getTotal());
        assertEquals(2, register.getSalesCount());
    }
}
