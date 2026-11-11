package online.entreprenly.platform.sales.support;

import online.entreprenly.platform.sales.domain.model.aggregates.CashRegister;
import online.entreprenly.platform.sales.domain.repositories.CashRegisterRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class InMemoryCashRegisterRepository implements CashRegisterRepository {

    private final List<CashRegister> registers = new ArrayList<>();
    private long nextId = 1;

    @Override
    public Optional<CashRegister> findByOwnerEmailAndDate(String ownerEmail, LocalDate date) {
        return registers.stream()
                .filter(r -> Objects.equals(r.getOwnerEmail(), ownerEmail) && Objects.equals(r.getDate(), date))
                .findFirst();
    }

    @Override
    public Optional<CashRegister> findByOwnerEmailAndSellerIdAndDate(String ownerEmail, Long sellerId, LocalDate date) {
        return registers.stream()
                .filter(r -> Objects.equals(r.getOwnerEmail(), ownerEmail)
                        && Objects.equals(r.getSellerId(), sellerId)
                        && Objects.equals(r.getDate(), date))
                .findFirst();
    }

    @Override
    public List<CashRegister> findAllByOwnerEmail(String ownerEmail) {
        return registers.stream()
                .filter(r -> Objects.equals(r.getOwnerEmail(), ownerEmail))
                .toList();
    }

    @Override
    public CashRegister save(CashRegister cashRegister) {
        if (cashRegister.getId() == null) {
            cashRegister.setId(nextId++);
            registers.add(cashRegister);
        } else {
            registers.removeIf(r -> Objects.equals(r.getId(), cashRegister.getId()));
            registers.add(cashRegister);
        }
        return cashRegister;
    }

    public List<CashRegister> all() {
        return List.copyOf(registers);
    }
}
