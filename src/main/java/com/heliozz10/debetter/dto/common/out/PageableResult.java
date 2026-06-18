package com.heliozz10.debetter.dto.common.out;

import java.util.Collection;

public record PageableResult<T>(Collection<T> content, long totalElements, long totalPages) {
}
