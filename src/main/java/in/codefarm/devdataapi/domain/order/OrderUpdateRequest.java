package in.codefarm.devdataapi.domain.order;

import java.math.BigDecimal;

public record OrderUpdateRequest(
        Long customerId,
        String status,
        BigDecimal total
) {}
