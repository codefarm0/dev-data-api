package in.codefarm.devdataapi.domain.order;

import java.math.BigDecimal;

public record Order(
        Long id,
        Long customerId,
        String status,
        BigDecimal total
) {
    public Order {
        if (status == null || status.isBlank()) {
            status = "PENDING";
        }
    }
}
