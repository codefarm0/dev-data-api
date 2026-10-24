package in.codefarm.devdataapi.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import in.codefarm.devdataapi.domain.product.Product;
import in.codefarm.devdataapi.domain.product.ProductCreateRequest;
import in.codefarm.devdataapi.domain.product.ProductUpdateRequest;
import in.codefarm.devdataapi.repository.ProductRepository;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/products")
    public List<Product> listProducts() {
        return productRepository.findAll();
    }

    @GetMapping("/products/{id}")
    public Product getProduct(@PathVariable Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id));
    }

    @PostMapping("/products")
    public ResponseEntity<Product> createProduct(@RequestBody ProductCreateRequest request) {
        Product product = productRepository.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    @PutMapping("/products/{id}")
    public Product replaceProduct(@PathVariable Long id, @RequestBody ProductUpdateRequest request) {
        return productRepository.update(id, request);
    }

    @PatchMapping("/products/{id}")
    public Product patchProduct(@PathVariable Long id, @RequestBody ProductUpdateRequest request) {
        return productRepository.update(id, request);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        boolean deleted = productRepository.delete(id);
        if (!deleted) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id);
        }
        return ResponseEntity.noContent().build();
    }
}
