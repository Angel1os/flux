package com.angellos.shared.record;

import java.util.UUID;

public record Params(
        String searchValue,
        String name,
        String status,
        String type,
        boolean paginate,
        Integer page,
        Integer pageSize,
        String startDate,
        String endDate,
        UUID userId
) {
}
