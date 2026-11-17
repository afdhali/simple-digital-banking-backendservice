package tutorjava.digitalbanking.account;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tutorjava.digitalbanking.account.dto.AccountResponse;
import tutorjava.digitalbanking.account.dto.CreateAccountRequest;
import tutorjava.digitalbanking.common.response.ApiError;
import tutorjava.digitalbanking.config.OpenApiConfig;
import tutorjava.digitalbanking.security.AuthenticatedUser;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "Open a new account (starts with zero balance)")
    @ApiResponse(responseCode = "422", description = "Account limit reached", content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<AccountResponse> open(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateAccountRequest request) {
        AccountResponse created = accountService.open(principal.id(), request);
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + created.accountNumber())).body(created);
    }

    @GetMapping
    @Operation(summary = "List my accounts")
    public List<AccountResponse> list(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal) {
        return accountService.listOwn(principal.id());
    }

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Get one of my accounts (including balance)")
    @ApiResponse(responseCode = "404", description = "Account not found or not owned by the caller", content = @Content(schema = @Schema(implementation = ApiError.class)))
    public AccountResponse get(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String accountNumber) {
        return accountService.getOwn(principal.id(), accountNumber);
    }
}
