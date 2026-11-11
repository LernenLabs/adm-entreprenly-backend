package online.entreprenly.platform.sales.application.queryservices;

import online.entreprenly.platform.sales.domain.model.aggregates.CashRegister;
import online.entreprenly.platform.sales.domain.model.queries.GetCashRegisterByDateQuery;

/**
 * Application service contract for querying daily cash registers.
 */
public interface CashRegisterQueryService {

    /**
     * Resolves the daily cash register for the requested date, deriving and persisting
     * initial totals from sales if no register has been initialized yet for that day.
     *
     * @param query the query containing owner email, date, and optional sellerId
     * @return the cash register aggregate with accumulated totals
     */
    CashRegister handle(GetCashRegisterByDateQuery query);
}

