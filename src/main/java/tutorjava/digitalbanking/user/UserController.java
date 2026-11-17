package tutorjava.digitalbanking.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tutorjava.digitalbanking.config.OpenApiConfig;
import tutorjava.digitalbanking.security.AuthenticatedUser;
import tutorjava.digitalbanking.user.dto.UserResponse;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get the profile of the authenticated user")
    public UserResponse me(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return userService.getProfile(principal.id());
    }
}
