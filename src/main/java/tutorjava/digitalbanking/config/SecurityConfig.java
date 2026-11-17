package tutorjava.digitalbanking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tutorjava.digitalbanking.security.JwtAuthenticationFilter;
import tutorjava.digitalbanking.security.JwtService;
import tutorjava.digitalbanking.security.RestAccessDeniedHandler;
import tutorjava.digitalbanking.security.RestAuthenticationEntryPoint;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private static final String[] PUBLIC_ENDPOINTS = {
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/logout",
                        "/actuator/health/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**"
        };

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http,
                        JwtService jwtService,
                        RestAuthenticationEntryPoint entryPoint,
                        RestAccessDeniedHandler accessDeniedHandler,
                        CorsConfigurationSource corsConfigurationSource)
                        throws Exception {
                http
                                // Stateless bearer-token API: no cookies/session, so CSRF protection is not
                                // applicable.
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .formLogin(AbstractHttpConfigurer::disable)
                                .httpBasic(AbstractHttpConfigurer::disable)
                                .logout(AbstractHttpConfigurer::disable)
                                .headers(headers -> headers
                                                .frameOptions(frame -> frame.deny())
                                                .httpStrictTransportSecurity(hsts -> hsts
                                                                .includeSubDomains(true)
                                                                .maxAgeInSeconds(31_536_000))
                                                .referrerPolicy(ref -> ref.policy(ReferrerPolicy.NO_REFERRER))
                                                .contentTypeOptions(Customizer.withDefaults()))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                                                .anyRequest().authenticated())
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(entryPoint)
                                                .accessDeniedHandler(accessDeniedHandler))
                                .addFilterBefore(new JwtAuthenticationFilter(jwtService),
                                                UsernamePasswordAuthenticationFilter.class);
                return http.build();
        }

        /** BCrypt with cost 12 (adaptive hash with per-password salt). */
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(12);
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(properties.allowedOrigins());
                config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
                config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "X-Request-Id",
                                "X-Client-Type"));
                config.setExposedHeaders(List.of("X-Request-Id", "Idempotent-Replayed", "Location"));
                config.setAllowCredentials(true); // browsers must be allowed to send the refresh-token cookie
                config.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/api/**", config);
                return source;
        }
}
