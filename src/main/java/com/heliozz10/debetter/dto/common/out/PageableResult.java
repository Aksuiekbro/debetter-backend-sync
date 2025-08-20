package com.heliozz10.debetter.dto.common.out;

import java.util.List;

public record PageableResult<T>(List<T> content, long totalElements, long totalPages) {
}
