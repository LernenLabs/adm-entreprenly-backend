package online.entreprenly.platform.sales.application.internal.commandservices;

import online.entreprenly.platform.sales.application.commandservices.SaleCommandService;
import online.entreprenly.platform.sales.domain.model.aggregates.CashRegister;
import online.entreprenly.platform.sales.domain.model.aggregates.Sale;
import online.entreprenly.platform.sales.domain.model.commands.CreateSaleCommand;
import online.entreprenly.platform.sales.domain.model.valueobjects.SaleStatus;
import online.entreprenly.platform.sales.domain.repositories.CashRegisterRepository;
import online.entreprenly.platform.sales.domain.repositories.SaleRepository;
import online.entreprenly.platform.shared.application.result.ApplicationError;
import online.entreprenly.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Sale command service implementation.
 *
 * <p>Registers sales and maintains the daily accumulated cash register by payment method
 * (cash vs digital channels), ensuring immediate consistency for point-of-sale operations.</p>
 */
@Service
public class SaleCommandServiceImpl implements SaleCommandService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Lima");

    private final SaleRepository saleRepository;
    private final CashRegisterRepository cashRegisterRepository;

    public SaleCommandServiceImpl(SaleRepository saleRepository,
                                  CashRegisterRepository cashRegisterRepository) {
        this.saleRepository = saleRepository;
        this.cashRegisterRepository = cashRegisterRepository;
    }

    @Override
    @Transactional
    public Result<Sale, ApplicationError> handle(CreateSaleCommand command) {
        if (command.ownerEmail() == null || command.ownerEmail().isBlank()) {
            return Result.failure(ApplicationError.validationError("owner", "An authenticated owner is required"));
        }
        if (command.sellerId() == null) {
            return Result.failure(ApplicationError.validationError("sellerId", "A seller is required to register a sale"));
        }
        if (command.items() == null || command.items().isEmpty()) {
            return Result.failure(ApplicationError.validationError("items", "A sale must contain at least one item"));
        }
        var sale = new Sale(command.ownerEmail(), command.sellerId(), command.items(), command.paymentMethod(),
                command.paymentReceipt(), command.status());
        var savedSale = saleRepository.save(sale);

        // Update the accumulated daily cash register by payment method
        accumulateCashRegister(savedSale);

        return Result.success(savedSale);
    }

    private void accumulateCashRegister(Sale sale) {
        if (sale == null || sale.getStatus() == SaleStatus.CANCELLED) {
            return;
        }
        var saleInstant = sale.getCreatedAt() != null ? sale.getCreatedAt() : Instant.now();
        var businessDate = saleInstant.atZone(BUSINESS_ZONE).toLocalDate();

        var register = cashRegisterRepository
                .findByOwnerEmailAndSellerIdAndDate(sale.getOwnerEmail(), sale.getSellerId(), businessDate)
                .orElseGet(() -> new CashRegister(sale.getOwnerEmail(), sale.getSellerId(), businessDate));

        register.recordSale(sale.getPaymentMethod(), sale.getTotal());
        cashRegisterRepository.save(register);
    }
}
