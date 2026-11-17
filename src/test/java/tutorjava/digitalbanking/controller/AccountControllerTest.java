package tutorjava.digitalbanking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tutorjava.digitalbanking.account.AccountController;
import tutorjava.digitalbanking.account.AccountService;
import tutorjava.digitalbanking.account.AccountStatus;
import tutorjava.digitalbanking.account.dto.AccountResponse;
import tutorjava.digitalbanking.account.dto.CreateAccountRequest;
import tutorjava.digitalbanking.security.AuthenticatedUser;
import tutorjava.digitalbanking.security.JwtAuthenticationFilter;
import tutorjava.digitalbanking.security.JwtService;

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

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AccountService accountService;

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
    @DisplayName("POST /api/v1/accounts - Berhasil membuka rekening baru dengan saldo 0")
    void openAccount_Success() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest("Tabungan Utama");
        AccountResponse response = new AccountResponse(
                "1234567890",
                "Tabungan Utama",
                "IDR",
                BigDecimal.ZERO,
                AccountStatus.ACTIVE,
                Instant.now());

        when(accountService.open(eq(userId), any(CreateAccountRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/accounts/1234567890"))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.accountName").value("Tabungan Utama"))
                .andExpect(jsonPath("$.currency").value("IDR"))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/v1/accounts - Berhasil mengambil daftar rekening milik nasabah")
    void listAccounts_Success() throws Exception {
        AccountResponse account1 = new AccountResponse(
                "1234567890",
                "Tabungan Utama",
                "IDR",
                new BigDecimal("1500000.00"),
                AccountStatus.ACTIVE,
                Instant.now());
        AccountResponse account2 = new AccountResponse(
                "0987654321",
                "Tabungan Liburan",
                "IDR",
                new BigDecimal("500000.00"),
                AccountStatus.ACTIVE,
                Instant.now());

        when(accountService.listOwn(userId)).thenReturn(List.of(account1, account2));

        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].accountNumber").value("1234567890"))
                .andExpect(jsonPath("$[0].balance").value(1500000.00))
                .andExpect(jsonPath("$[1].accountNumber").value("0987654321"));
    }

    @Test
    @DisplayName("GET /api/v1/accounts/{accountNumber} - Berhasil mengambil detail satu rekening")
    void getAccount_Success() throws Exception {
        AccountResponse response = new AccountResponse(
                "1234567890",
                "Tabungan Utama",
                "IDR",
                new BigDecimal("1500000.00"),
                AccountStatus.ACTIVE,
                Instant.now());

        when(accountService.getOwn(userId, "1234567890")).thenReturn(response);

        mockMvc.perform(get("/api/v1/accounts/1234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.accountName").value("Tabungan Utama"))
                .andExpect(jsonPath("$.balance").value(1500000.00));
    }
}