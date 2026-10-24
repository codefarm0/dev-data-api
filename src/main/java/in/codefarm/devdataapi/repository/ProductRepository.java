package in.codefarm.devdataapi.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import in.codefarm.devdataapi.domain.product.Product;
import in.codefarm.devdataapi.domain.product.ProductCreateRequest;
import in.codefarm.devdataapi.domain.product.ProductUpdateRequest;
import in.codefarm.devdataapi.storage.JsonResourceRepository;

@Repository
public class ProductRepository {

    private static final String RESOURCE_NAME = "products";

    private final JsonResourceRepository repository;

    public ProductRepository(JsonResourceRepository repository) {
        this.repository = repository;
    }

    public List<Product> findAll() {
        return repository.readList(RESOURCE_NAME, Product.class).stream()
                .sorted(Comparator.comparing(Product::id))
                .toList();
    }

    public Optional<Product> findById(Long id) {
        return findAll().stream()
                .filter(product -> product.id().equals(id))
                .findFirst();
    }

    public Product create(ProductCreateRequest request) {
        List<Product> products = new ArrayList<>(findAll());
        long nextId = products.stream()
                .map(Product::id)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L) + 1L;

        Product product = new Product(nextId, request.name(), request.category(), request.status(), request.price());
        if (products.stream().anyMatch(existing -> existing.id().equals(product.id()))) {
            throw new IllegalStateException("Duplicate product id detected: " + product.id());
        }

        products.add(product);
        repository.writeList(RESOURCE_NAME, products);
        return product;
    }

    public Product update(Long id, ProductUpdateRequest request) {
        List<Product> products = new ArrayList<>(findAll());
        for (int i = 0; i < products.size(); i++) {
            Product current = products.get(i);
            if (current.id().equals(id)) {
                Product updated = new Product(id, request.name(), request.category(), request.status(), request.price());
                products.set(i, updated);
                repository.writeList(RESOURCE_NAME, products);
                return updated;
            }
        }
        throw new IllegalArgumentException("Product not found: " + id);
    }

    public boolean delete(Long id) {
        List<Product> products = new ArrayList<>(findAll());
        boolean removed = products.removeIf(product -> product.id().equals(id));
        if (removed) {
            repository.writeList(RESOURCE_NAME, products);
        }
        return removed;
    }
}
