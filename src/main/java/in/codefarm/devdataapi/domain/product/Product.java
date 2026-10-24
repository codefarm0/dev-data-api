package in.codefarm.devdataapi.domain.product;

import java.math.BigDecimal;

public record Product(
        Long id,
        String name,
        String category,
        String status,
        BigDecimal price
) {
    public Product {
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }
}
