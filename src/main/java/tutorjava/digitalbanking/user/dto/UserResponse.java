package tutorjava.digitalbanking.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tutorjava.digitalbanking.user.User;
import tutorjava.digitalbanking.user.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        @Schema(example = "budi@example.com") String email,
        @Schema(example = "Budi Santoso") String fullName,
        UserRole role,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.getRole(), user.getCreatedAt());
    }
}
