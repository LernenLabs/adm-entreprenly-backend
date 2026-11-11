package online.entreprenly.platform.sales.support;

import online.entreprenly.platform.sales.domain.model.aggregates.Sale;
import online.entreprenly.platform.sales.domain.repositories.SaleRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class InMemorySaleRepository implements SaleRepository {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Lima");

    private final List<Sale> sales = new ArrayList<>();
    private long nextId = 1;

    @Override
    public List<Sale> findAllByOwnerEmail(String ownerEmail) {
        return sales.stream()
                .filter(s -> Objects.equals(s.getOwnerEmail(), ownerEmail))
                .toList();
    }

    @Override
    public List<Sale> findAllByOwnerEmailAndDate(String ownerEmail, LocalDate date) {
        return sales.stream()
                .filter(s -> Objects.equals(s.getOwnerEmail(), ownerEmail))
                .filter(s -> {
                    if (s.getCreatedAt() == null) return false;
                    var saleDate = s.getCreatedAt().atZone(BUSINESS_ZONE).toLocalDate();
                    return Objects.equals(saleDate, date);
                })
                .toList();
    }

    @Override
    public Optional<Sale> findByIdAndOwnerEmail(Long id, String ownerEmail) {
        return sales.stream()
                .filter(s -> Objects.equals(s.getId(), id) && Objects.equals(s.getOwnerEmail(), ownerEmail))
                .findFirst();
    }

    @Override
    public Sale save(Sale sale) {
        if (sale.getId() == null) {
            sale.setId(nextId++);
            sales.add(sale);
        } else {
            sales.removeIf(s -> Objects.equals(s.getId(), sale.getId()));
            sales.add(sale);
        }
        return sale;
    }

    public List<Sale> all() {
        return List.copyOf(sales);
    }
}
