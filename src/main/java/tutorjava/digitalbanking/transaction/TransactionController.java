package tutorjava.digitalbanking.transaction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tutorjava.digitalbanking.common.response.ApiError;
import tutorjava.digitalbanking.common.response.PageResponse;
import tutorjava.digitalbanking.config.OpenApiConfig;
import tutorjava.digitalbanking.idempotency.IdempotencyService;
import tutorjava.digitalbanking.security.AuthenticatedUser;
import tutorjava.digitalbanking.transaction.dto.AccountMutationResponse;
import tutorjava.digitalbanking.transaction.dto.DepositRequest;
import tutorjava.digitalbanking.transaction.dto.TransactionResponse;
import tutorjava.digitalbanking.transaction.dto.TransferRequest;
import tutorjava.digitalbanking.transaction.dto.WithdrawalRequest;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Money movements (idempotent) and statements")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class TransactionController {

    private static final String REPLAY_HEADER = "Idempotent-Replayed";
    private static final String KEY_DESCRIPTION =
            "Unique key per logical operation (e.g. a UUID). Retrying with the same key and payload "
                    + "returns the original result without moving money again.";

    private final TransactionService transactionService;
    private final TransactionQueryService queryService;

    @PostMapping("/transactions/deposits")
    @Operation(summary = "Deposit into one of my accounts")
    @ApiResponse(responseCode = "201", description = "Deposit executed")
    @ApiResponse(responseCode = "200", description = "Idempotent replay of an earlier identical request",
            headers = @Header(name = REPLAY_HEADER, description = "always 'true'"))
    @ApiResponse(responseCode = "422", description = "Business rule violated / key reused with another payload",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<TransactionResponse> deposit(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @Parameter(description = KEY_DESCRIPTION, example = "6f1c2a52-8a4e-4f0e-9a0b-3d6c5f6a1e21")
            @RequestHeader(IdempotencyService.HEADER) String idempotencyKey,
            @Valid @RequestBody DepositRequest request) {
        return toResponse(transactionService.execute(idempotencyKey,
                TransactionCommand.deposit(principal.id(), request.accountNumber(),
                        request.amount(), request.description())));
    }

    @PostMapping("/transactions/withdrawals")
    @Operation(summary = "Withdraw from one of my accounts")
    @ApiResponse(responseCode = "201", description = "Withdrawal executed")
    @ApiResponse(responseCode = "200", description = "Idempotent replay of an earlier identical request",
            headers = @Header(name = REPLAY_HEADER, description = "always 'true'"))
    @ApiResponse(responseCode = "422", description = "Insufficient balance / limit exceeded / key reused",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<TransactionResponse> withdraw(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @Parameter(description = KEY_DESCRIPTION, example = "0d4a7c19-3f5b-4d58-8d52-7c1f9e2b6a10")
            @RequestHeader(IdempotencyService.HEADER) String idempotencyKey,
            @Valid @RequestBody WithdrawalRequest request) {
        return toResponse(transactionService.execute(idempotencyKey,
                TransactionCommand.withdrawal(principal.id(), request.accountNumber(),
                        request.amount(), request.description())));
    }

    @PostMapping("/transactions/transfers")
    @Operation(summary = "Transfer from one of my accounts to another account")
    @ApiResponse(responseCode = "201", description = "Transfer executed")
    @ApiResponse(responseCode = "200", description = "Idempotent replay of an earlier identical request",
            headers = @Header(name = REPLAY_HEADER, description = "always 'true'"))
    @ApiResponse(responseCode = "404", description = "Source (not mine) or destination account not found",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "422", description = "Insufficient balance / same account / key reused",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<TransactionResponse> transfer(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @Parameter(description = KEY_DESCRIPTION, example = "b2e8f5c0-6a1d-4c36-9f47-1a2b3c4d5e6f")
            @RequestHeader(IdempotencyService.HEADER) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        return toResponse(transactionService.execute(idempotencyKey,
                TransactionCommand.transfer(principal.id(), request.sourceAccountNumber(),
                        request.destinationAccountNumber(), request.amount(), request.description())));
    }

    @GetMapping("/transactions/{referenceNo}")
    @Operation(summary = "Get a transaction I took part in (as sender or receiver)")
    @ApiResponse(responseCode = "404", description = "Not found or not visible to the caller",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public TransactionResponse getTransaction(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String referenceNo) {
        return queryService.getByReference(principal.id(), referenceNo);
    }

    @GetMapping("/accounts/{accountNumber}/transactions")
    @Operation(summary = "Account statement (newest first, paginated, with running balance)")
    @ApiResponse(responseCode = "404", description = "Account not found or not owned by the caller",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public PageResponse<AccountMutationResponse> statement(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String accountNumber,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, 1-100") @RequestParam(defaultValue = "20") int size) {
        return queryService.getStatement(principal.id(), accountNumber, page, size);
    }

    private ResponseEntity<TransactionResponse> toResponse(TransactionResult result) {
        TransactionResponse body = result.response();
        if (result.replayed()) {
            return ResponseEntity.ok().header(REPLAY_HEADER, "true").body(body);
        }
        return ResponseEntity.created(URI.create("/api/v1/transactions/" + body.referenceNo())).body(body);
    }
}
