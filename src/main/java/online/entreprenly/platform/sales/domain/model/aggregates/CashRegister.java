package online.entreprenly.platform.sales.domain.model.aggregates;

import lombok.Getter;
import lombok.Setter;
import online.entreprenly.platform.sales.domain.model.valueobjects.PaymentMethod;
import online.entreprenly.platform.sales.domain.model.valueobjects.SaleStatus;
import online.entreprenly.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * CashRegister aggregate root.
 *
 * <p>Represents the daily accumulated cash register for a merchant ({@code ownerEmail})
 * and seller ({@code sellerId}) for a specific business date. It tracks accumulated sales
 * broken down by payment method (cash vs digital channels like Yape, Plin, Card), the
 * count of transactions, and the grand total.</p>
 */
@Getter
public class CashRegister extends AbstractDomainAggregateRoot<CashRegister> {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Lima");

    @Setter
    private Long id;
    private String ownerEmail;
    private Long sellerId;
    private LocalDate date;
    private double cashTotal;
    private double yapeTotal;
    private double plinTotal;
    private double cardTotal;
    private double digitalTotal;
    private double total;
    private int salesCount;
    private int cashSalesCount;
    private int digitalSalesCount;

    public CashRegister() {
    }

    public CashRegister(String ownerEmail, Long sellerId, LocalDate date) {
        this.ownerEmail = ownerEmail;
        this.sellerId = sellerId == null ? 1L : sellerId;
        this.date = date == null ? LocalDate.now(BUSINESS_ZONE) : date;
        this.cashTotal = 0d;
        this.yapeTotal = 0d;
        this.plinTotal = 0d;
        this.cardTotal = 0d;
        this.digitalTotal = 0d;
        this.total = 0d;
        this.salesCount = 0;
        this.cashSalesCount = 0;
        this.digitalSalesCount = 0;
    }

    /**
     * Factory method creating a fresh, zeroed cash register for the given owner, seller, and date.
     */
    public static CashRegister empty(String ownerEmail, Long sellerId, LocalDate date) {
        return new CashRegister(ownerEmail, sellerId, date);
    }

    /**
     * Factory method creating and computing a cash register from an existing list of sales.
     */
    public static CashRegister fromSales(String ownerEmail, Long sellerId, LocalDate date, List<Sale> sales) {
        var register = new CashRegister(ownerEmail, sellerId, date);
        if (sales != null) {
            for (var sale : sales) {
                if (sale.getStatus() == null || sale.getStatus() != SaleStatus.CANCELLED) {
                    register.recordSale(sale.getPaymentMethod(), sale.getTotal());
                }
            }
        }
        return register;
    }

    /**
     * Records a single sale amount under its payment method and updates all derived totals.
     *
     * @param paymentMethod the payment method used (defaults to CASH if null)
     * @param amount        the sale total amount to accumulate
     */
    public void recordSale(PaymentMethod paymentMethod, double amount) {
        if (amount <= 0) {
            return;
        }
        var method = paymentMethod == null ? PaymentMethod.CASH : paymentMethod;
        double roundedAmount = round(amount);

        switch (method) {
            case CASH -> {
                this.cashTotal = round(this.cashTotal + roundedAmount);
                this.cashSalesCount++;
            }
            case YAPE -> {
                this.yapeTotal = round(this.yapeTotal + roundedAmount);
                this.digitalTotal = round(this.digitalTotal + roundedAmount);
                this.digitalSalesCount++;
            }
            case PLIN -> {
                this.plinTotal = round(this.plinTotal + roundedAmount);
                this.digitalTotal = round(this.digitalTotal + roundedAmount);
                this.digitalSalesCount++;
            }
            case CARD -> {
                this.cardTotal = round(this.cardTotal + roundedAmount);
                this.digitalTotal = round(this.digitalTotal + roundedAmount);
                this.digitalSalesCount++;
            }
        }

        this.total = round(this.cashTotal + this.digitalTotal);
        this.salesCount = this.cashSalesCount + this.digitalSalesCount;
    }

    /**
     * Recalculates all totals from a fresh list of sales (excluding cancelled sales).
     */
    public void recalculateFromSales(List<Sale> sales) {
        this.cashTotal = 0d;
        this.yapeTotal = 0d;
        this.plinTotal = 0d;
        this.cardTotal = 0d;
        this.digitalTotal = 0d;
        this.total = 0d;
        this.salesCount = 0;
        this.cashSalesCount = 0;
        this.digitalSalesCount = 0;

        if (sales != null) {
            for (var sale : sales) {
                if (sale.getStatus() == null || sale.getStatus() != SaleStatus.CANCELLED) {
                    recordSale(sale.getPaymentMethod(), sale.getTotal());
                }
            }
        }
    }

    /**
     * Restores an aggregate from persistence.
     */
    public void restoreState(Long id, String ownerEmail, Long sellerId, LocalDate date,
                             double cashTotal, double yapeTotal, double plinTotal, double cardTotal,
                             double digitalTotal, double total, int salesCount,
                             int cashSalesCount, int digitalSalesCount) {
        this.id = id;
        this.ownerEmail = ownerEmail;
        this.sellerId = sellerId;
        this.date = date;
        this.cashTotal = cashTotal;
        this.yapeTotal = yapeTotal;
        this.plinTotal = plinTotal;
        this.cardTotal = cardTotal;
        this.digitalTotal = digitalTotal;
        this.total = total;
        this.salesCount = salesCount;
        this.cashSalesCount = cashSalesCount;
        this.digitalSalesCount = digitalSalesCount;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

