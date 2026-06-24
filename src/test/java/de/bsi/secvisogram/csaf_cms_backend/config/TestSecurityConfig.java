package de.bsi.secvisogram.csaf_cms_backend.config;

import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Test configuration that provides a {@link JwtDecoder} bean so that the
 * Spring Security OAuth2 resource server can start up without a running
 * Keycloak / OIDC provider during integration tests (Spring Boot 4 / Spring
 * Security 7 require an explicit {@link JwtDecoder} when a custom
 * {@link org.springframework.security.web.SecurityFilterChain} is present).
 *
 * <p>This class lives in {@code src/test/java} and is therefore only active
 * during tests.  It is in the same package as the production
 * {@link SecurityConfig} so that Spring component scanning picks it up
 * automatically when the full application context is loaded by
 * {@code @SpringBootTest}.
 */
@Configuration
public class TestSecurityConfig {

    @Bean
    JwtDecoder jwtDecoder() {
        // A simple symmetric HMAC decoder. Tokens are never validated against a
        // real OIDC issuer in tests, so the key material does not matter.
        byte[] secret = "test-secret-key-for-integration-tests-only-32b".getBytes();
        SecretKeySpec secretKey = new SecretKeySpec(secret, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }
}
