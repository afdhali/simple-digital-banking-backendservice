package tutorjava.digitalbanking.common.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable pagination envelope (does not leak Spring Data's Page serialization
 * format).
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
