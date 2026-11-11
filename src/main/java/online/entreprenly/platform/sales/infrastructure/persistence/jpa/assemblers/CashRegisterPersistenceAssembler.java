package online.entreprenly.platform.sales.infrastructure.persistence.jpa.assemblers;

import online.entreprenly.platform.sales.domain.model.aggregates.CashRegister;
import online.entreprenly.platform.sales.infrastructure.persistence.jpa.entities.CashRegisterPersistenceEntity;

/**
 * Static assembler between CashRegister domain and persistence representations.
 */
public final class CashRegisterPersistenceAssembler {

    private CashRegisterPersistenceAssembler() {
    }

    public static CashRegister toDomainFromPersistence(CashRegisterPersistenceEntity entity) {
        if (entity == null) {
            return null;
        }
        var register = new CashRegister();
        register.restoreState(
                entity.getId(),
                entity.getOwnerEmail(),
                entity.getSellerId(),
                entity.getRegisterDate(),
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
        return register;
    }

    public static CashRegisterPersistenceEntity toPersistenceFromDomain(CashRegister register) {
        if (register == null) {
            return null;
        }
        var entity = new CashRegisterPersistenceEntity();
        if (register.getId() != null) {
            entity.setId(register.getId());
        }
        entity.setOwnerEmail(register.getOwnerEmail());
        entity.setSellerId(register.getSellerId());
        entity.setRegisterDate(register.getDate());
        entity.setCashTotal(register.getCashTotal());
        entity.setYapeTotal(register.getYapeTotal());
        entity.setPlinTotal(register.getPlinTotal());
        entity.setCardTotal(register.getCardTotal());
        entity.setDigitalTotal(register.getDigitalTotal());
        entity.setTotal(register.getTotal());
        entity.setSalesCount(register.getSalesCount());
        entity.setCashSalesCount(register.getCashSalesCount());
        entity.setDigitalSalesCount(register.getDigitalSalesCount());
        return entity;
    }
}

