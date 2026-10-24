package in.codefarm.devdataapi.domain.product;

import java.math.BigDecimal;

public record ProductCreateRequest(
        String name,
        String category,
        String status,
        BigDecimal price
) {
    public ProductCreateRequest {
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }
}
