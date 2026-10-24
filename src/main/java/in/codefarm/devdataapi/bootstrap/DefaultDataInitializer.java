package in.codefarm.devdataapi.bootstrap;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import in.codefarm.devdataapi.domain.product.Product;
import in.codefarm.devdataapi.domain.user.User;
import in.codefarm.devdataapi.storage.JsonResourceRepository;

@Component
public class DefaultDataInitializer {

    private final JsonResourceRepository repository;

    public DefaultDataInitializer(JsonResourceRepository repository) {
        this.repository = repository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        repository.warnIfDuplicateIds("users");
        repository.warnIfDuplicateIds("products");
        repository.warnIfDuplicateIds("orders");

        repository.ensureResource("users", List.of(
                new User(1L, "Aarav", "Sharma", "aarav.sharma@example.test", "ACTIVE"),
                new User(2L, "Meera", "Verma", "meera.verma@example.test", "ACTIVE")
        ));

        repository.ensureResource("products", List.of(
                new Product(1L, "Laptop Pro", "electronics", "ACTIVE", new BigDecimal("1299.99")),
                new Product(2L, "Desk Lamp", "home", "ACTIVE", new BigDecimal("49.99"))
        ));
    }
}
