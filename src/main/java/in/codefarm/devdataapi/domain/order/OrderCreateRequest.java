package in.codefarm.devdataapi.domain.order;

import java.math.BigDecimal;

public record OrderCreateRequest(
        Long customerId,
        String status,
        BigDecimal total
) {
    public OrderCreateRequest {
        if (status == null || status.isBlank()) {
            status = "PENDING";
        }
    }
}
