package online.entreprenly.platform.sales.interfaces.rest.transform;

import online.entreprenly.platform.sales.domain.model.aggregates.CashRegister;
import online.entreprenly.platform.sales.interfaces.rest.resources.CashRegisterResource;

/**
 * Assembler that transforms a CashRegister domain aggregate into its REST resource representation.
 */
public final class CashRegisterResourceFromEntityAssembler {

    private CashRegisterResourceFromEntityAssembler() {
    }

    public static CashRegisterResource toResourceFromEntity(CashRegister entity) {
        if (entity == null) {
            return null;
        }
        return new CashRegisterResource(
                entity.getId(),
                entity.getSellerId(),
                entity.getOwnerEmail(),
                entity.getDate(),
                entity.getCashTotal(),
                entity.getYapeTotal(),
                entity.getPlinTotal(),
                entity.getCardTotal(),
                entity.getDigitalTotal(),
                entity.getTotal(),
                entity.getSalesCount(),
                entity.getCashSalesCount(),
                entity.getDigitalSalesCount()
        );
    }
}
