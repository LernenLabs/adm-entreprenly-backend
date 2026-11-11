package online.entreprenly.platform.sales.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import online.entreprenly.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;

import java.time.LocalDate;

/**
 * JPA persistence entity for daily cash registers.
 *
 * <p>Stores the day's accumulated totals by payment method (cash and digital channels:
 * Yape, Plin, Card) along with transaction counts for a merchant ({@code owner_email})
 * and seller ({@code seller_id}).</p>
 */
@Entity
@Table(name = "cash_registers")
@Getter
@Setter
@NoArgsConstructor
public class CashRegisterPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "owner_email", nullable = false, length = 120)
    private String ownerEmail;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "register_date", nullable = false)
    private LocalDate registerDate;

    @Column(name = "cash_total", nullable = false)
    private double cashTotal;

    @Column(name = "yape_total", nullable = false)
    private double yapeTotal;

    @Column(name = "plin_total", nullable = false)
    private double plinTotal;

    @Column(name = "card_total", nullable = false)
    private double cardTotal;

    @Column(name = "digital_total", nullable = false)
    private double digitalTotal;

    @Column(name = "total", nullable = false)
    private double total;

    @Column(name = "sales_count", nullable = false)
    private int salesCount;

    @Column(name = "cash_sales_count", nullable = false)
    private int cashSalesCount;

    @Column(name = "digital_sales_count", nullable = false)
    private int digitalSalesCount;
}
