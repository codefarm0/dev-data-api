package in.codefarm.devdataapi.domain.product;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        String name,
        String category,
        String status,
        BigDecimal price
) {}
