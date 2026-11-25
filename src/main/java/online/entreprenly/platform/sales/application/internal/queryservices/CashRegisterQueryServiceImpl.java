package online.entreprenly.platform.sales.application.internal.queryservices;

import online.entreprenly.platform.sales.application.queryservices.CashRegisterQueryService;
import online.entreprenly.platform.sales.domain.model.aggregates.CashRegister;
import online.entreprenly.platform.sales.domain.model.queries.GetCashRegisterByDateQuery;
import online.entreprenly.platform.sales.domain.repositories.CashRegisterRepository;
import online.entreprenly.platform.sales.domain.repositories.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

/**
 * CashRegister query service implementation.
 *
 * <p>Resolves the daily cash register for an authenticated merchant. If a cash register
 * record already exists in the database for the given date, it returns it directly.
 * If not yet initialized (e.g. first query of the day), it computes the initial totals
 * from the day's sales (if any) and persists the register so subsequent queries hit the
 * accumulated record.</p>
 */
@Service
public class CashRegisterQueryServiceImpl implements CashRegisterQueryService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Lima");

    private final CashRegisterRepository cashRegisterRepository;
    private final SaleRepository saleRepository;

    public CashRegisterQueryServiceImpl(CashRegisterRepository cashRegisterRepository,
                                       SaleRepository saleRepository) {
        this.cashRegisterRepository = cashRegisterRepository;
        this.saleRepository = saleRepository;
    }

    @Override
    @Transactional
    public CashRegister handle(GetCashRegisterByDateQuery query) {
        LocalDate date = query.date() == null ? LocalDate.now(BUSINESS_ZONE) : query.date();
        String owner = query.ownerEmail();

        // 1. Look up existing persisted cash register
        Optional<CashRegister> existing = query.sellerId() != null
                ? cashRegisterRepository.findByOwnerEmailAndSellerIdAndDate(owner, query.sellerId(), date)
                : cashRegisterRepository.findByOwnerEmailAndDate(owner, date);

        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. If not yet persisted, compute from today's registered sales and persist
        var sales = saleRepository.findAllByOwnerEmailAndDate(owner, date);
        Long sellerId = query.sellerId() != null ? query.sellerId() : 1L;
        var computed = CashRegister.fromSales(owner, sellerId, date, sales);
        return cashRegisterRepository.save(computed);
    }
}

