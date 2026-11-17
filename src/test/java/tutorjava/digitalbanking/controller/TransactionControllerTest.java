package tutorjava.digitalbanking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tutorjava.digitalbanking.common.exception.BusinessException;
import tutorjava.digitalbanking.common.exception.ErrorCode;
import tutorjava.digitalbanking.common.exception.GlobalExceptionHandler;
import tutorjava.digitalbanking.common.response.PageResponse;
import tutorjava.digitalbanking.idempotency.IdempotencyService;
import tutorjava.digitalbanking.security.AuthenticatedUser;
import tutorjava.digitalbanking.security.JwtAuthenticationFilter;
import tutorjava.digitalbanking.security.JwtService;
import tutorjava.digitalbanking.transaction.*;
import tutorjava.digitalbanking.transaction.dto.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private TransactionQueryService queryService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        AuthenticatedUser principal = mock(AuthenticatedUser.class);
        when(principal.id()).thenReturn(userId);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null,
                List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /api/v1/transactions/deposits - Berhasil setor tunai (201 Created)")
    void deposit_Success() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        DepositRequest request = new DepositRequest("1234567890", new BigDecimal("500000.00"), "Top up");
        TransactionResponse txResponse = new TransactionResponse(
                "TRX20261117DEPOSIT01",
                TransactionType.DEPOSIT,
                new BigDecimal("500000.00"),
                "IDR",
                null,
                "1234567890",
                "Top up",
                Instant.now());

        when(transactionService.execute(eq(idempotencyKey), any(TransactionCommand.class)))
                .thenReturn(new TransactionResult(txResponse, false));

        mockMvc.perform(post("/api/v1/transactions/deposits")
                .header(IdempotencyService.HEADER, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/transactions/TRX20261117DEPOSIT01"))
                .andExpect(header().doesNotExist("Idempotent-Replayed"))
                .andExpect(jsonPath("$.referenceNo").value("TRX20261117DEPOSIT01"))
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(500000.00));
    }

    @Test
    @DisplayName("POST /api/v1/transactions/deposits - Idempotent replay mengembalikan 200 OK + header Idempotent-Replayed")
    void deposit_IdempotentReplay_Success() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        DepositRequest request = new DepositRequest("1234567890", new BigDecimal("500000.00"), "Top up");
        TransactionResponse txResponse = new TransactionResponse(
                "TRX20261117DEPOSIT01",
                TransactionType.DEPOSIT,
                new BigDecimal("500000.00"),
                "IDR",
                null,
                "1234567890",
                "Top up",
                Instant.now());

        when(transactionService.execute(eq(idempotencyKey), any(TransactionCommand.class)))
                .thenReturn(new TransactionResult(txResponse, true));

        mockMvc.perform(post("/api/v1/transactions/deposits")
                .header(IdempotencyService.HEADER, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.referenceNo").value("TRX20261117DEPOSIT01"));
    }

    @Test
    @DisplayName("POST /api/v1/transactions/withdrawals - Berhasil tarik tunai (201 Created)")
    void withdraw_Success() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        WithdrawalRequest request = new WithdrawalRequest("1234567890", new BigDecimal("100000.00"),
                "ATM cash withdrawal");
        TransactionResponse txResponse = new TransactionResponse(
                "TRX20261117WITHDRW01",
                TransactionType.WITHDRAWAL,
                new BigDecimal("100000.00"),
                "IDR",
                "1234567890",
                null,
                "ATM cash withdrawal",
                Instant.now());

        when(transactionService.execute(eq(idempotencyKey), any(TransactionCommand.class)))
                .thenReturn(new TransactionResult(txResponse, false));

        mockMvc.perform(post("/api/v1/transactions/withdrawals")
                .header(IdempotencyService.HEADER, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/transactions/TRX20261117WITHDRW01"))
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.amount").value(100000.00));
    }

    @Test
    @DisplayName("POST /api/v1/transactions/transfers - Berhasil transfer antarrekening (201 Created)")
    void transfer_Success() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        TransferRequest request = new TransferRequest(
                "1234567890",
                "9876543210",
                new BigDecimal("250000.00"),
                "Pay back lunch");
        TransactionResponse txResponse = new TransactionResponse(
                "TRX20261117TRANSFR01",
                TransactionType.TRANSFER,
                new BigDecimal("250000.00"),
                "IDR",
                "1234567890",
                "9876543210",
                "Pay back lunch",
                Instant.now());

        when(transactionService.execute(eq(idempotencyKey), any(TransactionCommand.class)))
                .thenReturn(new TransactionResult(txResponse, false));

        mockMvc.perform(post("/api/v1/transactions/transfers")
                .header(IdempotencyService.HEADER, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/transactions/TRX20261117TRANSFR01"))
                .andExpect(jsonPath("$.type").value("TRANSFER"))
                .andExpect(jsonPath("$.sourceAccountNumber").value("1234567890"))
                .andExpect(jsonPath("$.destinationAccountNumber").value("9876543210"));
    }

    @Test
    @DisplayName("POST /api/v1/transactions/transfers - Gagal ketika saldo tidak mencukupi (422 Unprocessable Entity)")
    void transfer_InsufficientBalance() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        TransferRequest request = new TransferRequest(
                "1234567890",
                "9876543210",
                new BigDecimal("5000000.00"),
                "Over balance");

        when(transactionService.execute(eq(idempotencyKey), any(TransactionCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.INSUFFICIENT_BALANCE));

        mockMvc.perform(post("/api/v1/transactions/transfers")
                .header(IdempotencyService.HEADER, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(422));
    }

    @Test
    @DisplayName("GET /api/v1/transactions/{referenceNo} - Berhasil mengambil detail transaksi")
    void getTransaction_Success() throws Exception {
        TransactionResponse txResponse = new TransactionResponse(
                "TRX20261117TRANSFR01",
                TransactionType.TRANSFER,
                new BigDecimal("250000.00"),
                "IDR",
                "1234567890",
                "9876543210",
                "Pay back lunch",
                Instant.now());

        when(queryService.getByReference(userId, "TRX20261117TRANSFR01")).thenReturn(txResponse);

        mockMvc.perform(get("/api/v1/transactions/TRX20261117TRANSFR01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceNo").value("TRX20261117TRANSFR01"))
                .andExpect(jsonPath("$.amount").value(250000.00));
    }

    @Test
    @DisplayName("GET /api/v1/accounts/{accountNumber}/transactions - Berhasil mengambil mutasi rekening")
    void getStatement_Success() throws Exception {
        AccountMutationResponse mutation = new AccountMutationResponse(
                "TRX20261117TRANSFR01",
                TransactionType.TRANSFER,
                EntryDirection.DEBIT,
                new BigDecimal("250000.00"),
                new BigDecimal("150000.00"),
                "Pay back lunch",
                Instant.now());
        PageResponse<AccountMutationResponse> pageResponse = new PageResponse<>(
                List.of(mutation), 0, 20, 1L, 1);

        when(queryService.getStatement(userId, "1234567890", 0, 20)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/accounts/1234567890/transactions")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].referenceNo").value("TRX20261117TRANSFR01"))
                .andExpect(jsonPath("$.content[0].direction").value("DEBIT"))
                .andExpect(jsonPath("$.content[0].balanceAfter").value(150000.00))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}