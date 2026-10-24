package in.codefarm.devdataapi.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import in.codefarm.devdataapi.domain.order.Order;
import in.codefarm.devdataapi.domain.order.OrderCreateRequest;
import in.codefarm.devdataapi.domain.order.OrderUpdateRequest;
import in.codefarm.devdataapi.storage.JsonResourceRepository;

@Repository
public class OrderRepository {

    private static final String RESOURCE_NAME = "orders";

    private final JsonResourceRepository repository;

    public OrderRepository(JsonResourceRepository repository) {
        this.repository = repository;
    }

    public List<Order> findAll() {
        return repository.readList(RESOURCE_NAME, Order.class).stream()
                .sorted(Comparator.comparing(Order::id))
                .toList();
    }

    public Optional<Order> findById(Long id) {
        return findAll().stream()
                .filter(order -> order.id().equals(id))
                .findFirst();
    }

    public Order create(OrderCreateRequest request) {
        List<Order> orders = new ArrayList<>(findAll());
        long nextId = orders.stream()
                .map(Order::id)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L) + 1L;

        Order order = new Order(nextId, request.customerId(), request.status(), request.total());
        if (orders.stream().anyMatch(existing -> existing.id().equals(order.id()))) {
            throw new IllegalStateException("Duplicate order id detected: " + order.id());
        }

        orders.add(order);
        repository.writeList(RESOURCE_NAME, orders);
        return order;
    }

    public Order update(Long id, OrderUpdateRequest request) {
        List<Order> orders = new ArrayList<>(findAll());
        for (int i = 0; i < orders.size(); i++) {
            Order current = orders.get(i);
            if (current.id().equals(id)) {
                Order updated = new Order(id, request.customerId(), request.status(), request.total());
                orders.set(i, updated);
                repository.writeList(RESOURCE_NAME, orders);
                return updated;
            }
        }
        throw new IllegalArgumentException("Order not found: " + id);
    }

    public boolean delete(Long id) {
        List<Order> orders = new ArrayList<>(findAll());
        boolean removed = orders.removeIf(order -> order.id().equals(id));
        if (removed) {
            repository.writeList(RESOURCE_NAME, orders);
        }
        return removed;
    }
}
