package com.openfeign.appointment_service.interceptor;


import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ================================================================
 * REQUEST INTERCEPTOR #1 — Authentication Header
 * ================================================================
 * A RequestInterceptor is Feign's pre-request hook.
 * It runs AFTER the RequestTemplate is fully built but BEFORE
 * the HTTP client sends the actual network request.
 * <p>
 * This interceptor adds: Authorization: Bearer <token>
 * to EVERY outbound Feign request from this service.
 * <p>
 * ── WHY @Component? ─────────────────────────────────────────────
 * Spring auto-discovers any bean that implements RequestInterceptor
 * and registers it with ALL Feign clients in this application context.
 * No extra wiring needed — just implement the interface + @Component.
 * <p>
 * If you only want this interceptor on one specific Feign client,
 * remove @Component and instead register it in that client's
 *
 * @FeignClient(configuration = SomeConfig.class) class.
 * <p>
 * ── EXECUTION ORDER ─────────────────────────────────────────────
 * Per-request Feign pipeline:
 * [1] SynchronousMethodHandler.invoke()
 * [2] RequestTemplate is built (URL, params, body from method args)
 * [3] ALL RequestInterceptors run in order  ← WE ARE HERE
 * [4] Target resolves the base URL
 * [5] HTTP client fires the request
 * [6] Response decoded / ErrorDecoder called
 * <p>
 * ── IN PRODUCTION ───────────────────────────────────────────────
 * Replace the static token with:
 * - OAuth2: pull token from OAuth2AuthorizedClientManager
 * - Spring Security: SecurityContextHolder.getContext().getAuthentication()
 * - Secret manager: pull from AWS Secrets Manager / Vault at runtime
 * ================================================================
 */

@Slf4j
@Component
public class AuthHeaderInterceptor implements RequestInterceptor {

    /**
     * @Value injects the property value from application.yml.
     * The colon (:) syntax provides a default value if the property is missing:
     * ${services.auth-token:demo-secret-token-healthcare-2024}
     * → uses "demo-secret-token-healthcare-2024" if services.auth-token not set.
     */
    @Value("${services.auth-token:demo-secret-token-healthcare-2024}")
    private String authToken;

    /**
     * apply() is called once per Feign request, just before the HTTP call.
     *
     * @param template The mutable HTTP request being assembled.
     *                 You can read: template.method(), template.url(), template.headers()
     *                 You can write: template.header(), template.query(), template.body()
     *                 <p>
     *                 template.header(key, values...) behavior:
     *                 - If the header already exists, it REPLACES existing values
     *                 - To append, read existing first: template.headers().get(key)
     */

    @Override
    public void apply(RequestTemplate template) {
        log.debug("[AuthInterceptor] Injecting Authorization header → {} {}",
                template.method(), template.url());
        template.header("Authorization", "Bearer " + authToken);
    }

}
