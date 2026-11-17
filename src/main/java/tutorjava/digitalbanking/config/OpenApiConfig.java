package tutorjava.digitalbanking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI digitalBankingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Digital Banking API")
                        .version("v1")
                        .description("""
                                Simple digital banking REST API.

                                **How to use:** register -> login -> click *Authorize* and paste the access token.
                                Every money movement (deposit, withdrawal, transfer) requires an `Idempotency-Key`
                                header (8-100 chars, e.g. a UUID). Retrying with the same key and the same payload
                                returns the original result instead of moving money twice."""))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
