package tutorjava.digitalbanking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tutorjava.digitalbanking.auth.AuthController;
import tutorjava.digitalbanking.auth.AuthResult;
import tutorjava.digitalbanking.auth.AuthService;
import tutorjava.digitalbanking.auth.IssuedRefreshToken;
import tutorjava.digitalbanking.auth.RefreshTokenCookies;
import tutorjava.digitalbanking.auth.dto.LoginRequest;
import tutorjava.digitalbanking.auth.dto.RefreshRequest;
import tutorjava.digitalbanking.auth.dto.RegisterRequest;
import tutorjava.digitalbanking.security.IssuedToken;
import tutorjava.digitalbanking.security.JwtAuthenticationFilter;
import tutorjava.digitalbanking.security.JwtService;
import tutorjava.digitalbanking.user.UserRole;
import tutorjava.digitalbanking.user.dto.UserResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // @Autowired
    // private ObjectMapper objectMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshTokenCookies cookies;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("POST /api/v1/auth/register - Berhasil registrasi nasabah baru")
    void register_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        RegisterRequest request = new RegisterRequest("budi@example.com", "Budi Santoso", "Str0ngPassw0rd!");
        UserResponse response = new UserResponse(userId, "budi@example.com", "Budi Santoso", UserRole.USER,
                Instant.now());

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("budi@example.com"))
                .andExpect(jsonPath("$.fullName").value("Budi Santoso"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login (Web) - Berhasil login dan set HttpOnly cookie")
    void login_Web_Success() throws Exception {
        LoginRequest request = new LoginRequest("budi@example.com", "Str0ngPassw0rd!");

        IssuedToken accessToken = mock(IssuedToken.class);
        when(accessToken.value()).thenReturn("mock-jwt-access-token");
        when(accessToken.expiresInSeconds()).thenReturn(900L);

        IssuedRefreshToken refreshToken = mock(IssuedRefreshToken.class);
        AuthResult authResult = new AuthResult(accessToken, refreshToken);

        ResponseCookie responseCookie = ResponseCookie.from("refresh_token", "mock-raw-refresh-token")
                .httpOnly(true)
                .path("/api/v1/auth")
                .maxAge(Duration.ofDays(7))
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResult);
        when(cookies.create(refreshToken)).thenReturn(responseCookie);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.accessToken").value("mock-jwt-access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login (Mobile) - Mengembalikan refreshToken di response body")
    void login_Mobile_Success() throws Exception {
        LoginRequest request = new LoginRequest("budi@example.com", "Str0ngPassw0rd!");

        IssuedToken accessToken = mock(IssuedToken.class);
        when(accessToken.value()).thenReturn("mock-jwt-access-token");
        when(accessToken.expiresInSeconds()).thenReturn(900L);

        IssuedRefreshToken refreshToken = mock(IssuedRefreshToken.class);
        when(refreshToken.value()).thenReturn("mobile-raw-refresh-token");

        AuthResult authResult = new AuthResult(accessToken, refreshToken);
        when(authService.login(any(LoginRequest.class))).thenReturn(authResult);

        mockMvc.perform(post("/api/v1/auth/login")
                .header("X-Client-Type", "mobile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.accessToken").value("mock-jwt-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("mobile-raw-refresh-token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh (Web) - Rotasi refresh token via cookie")
    void refresh_Web_Success() throws Exception {
        IssuedToken newAccessToken = mock(IssuedToken.class);
        when(newAccessToken.value()).thenReturn("rotated-access-token");
        when(newAccessToken.expiresInSeconds()).thenReturn(900L);

        IssuedRefreshToken newRefreshToken = mock(IssuedRefreshToken.class);
        AuthResult authResult = new AuthResult(newAccessToken, newRefreshToken);

        ResponseCookie rotatedCookie = ResponseCookie.from("refresh_token", "rotated-refresh-token")
                .httpOnly(true)
                .build();

        doNothing().when(cookies).requireTrustedOrigin(any(HttpServletRequest.class));
        when(cookies.read(any(HttpServletRequest.class))).thenReturn(Optional.of("old-refresh-token"));
        when(authService.refresh("old-refresh-token")).thenReturn(authResult);
        when(cookies.create(newRefreshToken)).thenReturn(rotatedCookie);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new Cookie("refresh_token", "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.accessToken").value("rotated-access-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh (Mobile) - Rotasi refresh token via JSON body")
    void refresh_Mobile_Success() throws Exception {
        RefreshRequest request = new RefreshRequest("old-mobile-token");

        IssuedToken newAccessToken = mock(IssuedToken.class);
        when(newAccessToken.value()).thenReturn("rotated-mobile-access");
        when(newAccessToken.expiresInSeconds()).thenReturn(900L);

        IssuedRefreshToken newRefreshToken = mock(IssuedRefreshToken.class);
        when(newRefreshToken.value()).thenReturn("rotated-mobile-refresh");

        when(authService.refresh("old-mobile-token")).thenReturn(new AuthResult(newAccessToken, newRefreshToken));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("rotated-mobile-access"))
                .andExpect(jsonPath("$.refreshToken").value("rotated-mobile-refresh"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout (Web) - Revoke sesi dan hapus cookie")
    void logout_Web_Success() throws Exception {
        ResponseCookie clearCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .maxAge(0)
                .build();

        doNothing().when(cookies).requireTrustedOrigin(any(HttpServletRequest.class));
        when(cookies.read(any(HttpServletRequest.class))).thenReturn(Optional.of("active-refresh-token"));
        doNothing().when(authService).logout("active-refresh-token");
        when(cookies.clear()).thenReturn(clearCookie);

        mockMvc.perform(post("/api/v1/auth/logout")
                .cookie(new Cookie("refresh_token", "active-refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));

        verify(authService).logout("active-refresh-token");
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout (Mobile) - Revoke sesi via JSON body")
    void logout_Mobile_Success() throws Exception {
        RefreshRequest request = new RefreshRequest("active-mobile-token");
        doNothing().when(authService).logout("active-mobile-token");

        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        verify(authService).logout("active-mobile-token");
    }
}