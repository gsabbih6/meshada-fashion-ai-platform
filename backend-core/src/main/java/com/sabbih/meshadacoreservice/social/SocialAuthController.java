package com.sabbih.meshadacoreservice.social;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/social/auth")
@Slf4j
public class SocialAuthController {

    private final SocialCredentialsRepository credentialsRepository;
    private final WebClient webClient;

    @Value("${spring.security.oauth2.client.registration.facebook.clientId:1314273177105590}")
    private String facebookClientId;

    @Value("${spring.security.oauth2.client.registration.facebook.clientSecret:0d0ae8aa8d7018eba40b0187b6a89428}")
    private String facebookClientSecret;

    @Value("${meshada.social.instagram.businessAccountId:17841464487692741}")
    private String instagramBusinessAccountId;

    @Autowired
    public SocialAuthController(SocialCredentialsRepository credentialsRepository, WebClient.Builder webClientBuilder) {
        this.credentialsRepository = credentialsRepository;
        this.webClient = webClientBuilder.build();
    }

    /**
     * Start the Instagram / Facebook OAuth flow.
     * Redirects the user to Facebook's authorization dialog.
     */
    @GetMapping("/instagram")
    public ResponseEntity<Void> redirectToFacebook(HttpServletRequest request) {
        String redirectUri = buildRedirectUri(request);
        log.info("[OAuth] Initiating Facebook login. Redirect URI: {}", redirectUri);

        String scope = "instagram_basic,instagram_content_publish,instagram_manage_comments,instagram_manage_messages,pages_show_list,pages_read_engagement";
        
        String authUrl = UriComponentsBuilder.fromHttpUrl("https://www.facebook.com/v19.0/dialog/oauth")
                .queryParam("client_id", facebookClientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", scope)
                .build()
                .toUriString();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authUrl))
                .build();
    }

    /**
     * Callback endpoint for Facebook OAuth redirect.
     * Receives the authorization code and exchanges it for a permanent Page Token.
     */
    @GetMapping("/instagram/callback")
    public Mono<ResponseEntity<String>> handleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false, name = "error_description") String errorDescription,
            HttpServletRequest request) {

        if (error != null) {
            log.error("[OAuth] Facebook authorization failed: {} - {}", error, errorDescription);
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("<h3>Facebook login failed</h3><p>" + errorDescription + "</p>"));
        }

        if (code == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("<h3>Facebook login failed</h3><p>No code returned from authorization server.</p>"));
        }

        String redirectUri = buildRedirectUri(request);
        log.info("[OAuth] Exchanging code for user token...");

        // 1. Exchange code for access token
        URI tokenUri = UriComponentsBuilder.fromHttpUrl("https://graph.facebook.com/v19.0/oauth/access_token")
                .queryParam("client_id", facebookClientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("client_secret", facebookClientSecret)
                .queryParam("code", code)
                .build()
                .toUri();
        
        return webClient.get()
                .uri(tokenUri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    String userToken = (String) response.get("access_token");
                    if (userToken == null) {
                        return Mono.error(new RuntimeException("Could not retrieve user access token from response: " + response));
                    }
                    
                    // 2. Exchange short-lived token for a long-lived (60 days) user token
                    log.info("[OAuth] Exchanging short-lived token for long-lived user token...");
                    URI exchangeUri = UriComponentsBuilder.fromHttpUrl("https://graph.facebook.com/v19.0/oauth/access_token")
                            .queryParam("grant_type", "fb_exchange_token")
                            .queryParam("client_id", facebookClientId)
                            .queryParam("client_secret", facebookClientSecret)
                            .queryParam("fb_exchange_token", userToken)
                            .build()
                            .toUri();
                    
                    return webClient.get()
                            .uri(exchangeUri)
                            .accept(MediaType.APPLICATION_JSON)
                            .retrieve()
                            .bodyToMono(Map.class);
                })
                .flatMap(response -> {
                    String longLivedUserToken = (String) response.get("access_token");
                    if (longLivedUserToken == null) {
                        return Mono.error(new RuntimeException("Could not retrieve long-lived user token."));
                    }
                    
                    // 3. Fetch linked Pages and their tokens & Instagram Business ID
                    log.info("[OAuth] Fetching linked Facebook Pages and Instagram accounts...");
                    URI accountsUri = UriComponentsBuilder.fromHttpUrl("https://graph.facebook.com/v19.0/me/accounts")
                            .queryParam("fields", "name,access_token,instagram_business_account{id,username}")
                            .queryParam("access_token", longLivedUserToken)
                            .build()
                            .toUri();
                    
                    return webClient.get()
                            .uri(accountsUri)
                            .accept(MediaType.APPLICATION_JSON)
                            .retrieve()
                            .bodyToMono(Map.class);
                })
                .map(response -> {
                    List<Map<String, Object>> pages = (List<Map<String, Object>>) response.get("data");
                    if (pages == null || pages.isEmpty()) {
                        return ResponseEntity.ok("<h3>Authorization Successful</h3><p>No Facebook Pages or Instagram Business Accounts linked to this Facebook account were found.</p>");
                    }

                    StringBuilder resultHtml = new StringBuilder();
                    resultHtml.append("<h3>Meshada Instagram Integration Setup</h3>");
                    resultHtml.append("<p>Successfully linked Facebook accounts. Detected pages:</p><ul>");

                    for (Map<String, Object> page : pages) {
                        String pageName = (String) page.get("name");
                        String pageAccessToken = (String) page.get("access_token");
                        Map<String, Object> igAccount = (Map<String, Object>) page.get("instagram_business_account");

                        resultHtml.append("<li><strong>").append(pageName).append("</strong>");
                        if (igAccount != null) {
                            String igId = (String) igAccount.get("id");
                            String igUsername = (String) igAccount.get("username");
                            resultHtml.append(" (Instagram: @").append(igUsername).append(")");

                            // Determine if this is the target profile.
                            // Must match the configured target ID, the default active ID "17841464487692741", 
                            // the username "meshadafashion", or be the only account available.
                            boolean isTarget = false;
                            if (instagramBusinessAccountId.equals(igId) || 
                                    "17841464487692741".equals(igId) || 
                                    "meshadafashion".equalsIgnoreCase(igUsername)) {
                                isTarget = true;
                            } else if (pages.size() == 1) {
                                isTarget = true;
                            }

                            if (isTarget) {
                                // Save Instagram credentials to DB under the active platform key
                                SocialCredentials credentials = SocialCredentials.builder()
                                        .platform("instagram")
                                        .accessToken(pageAccessToken)
                                        .businessAccountId(igId)
                                        .updatedAt(LocalDateTime.now())
                                        .build();
                                
                                credentialsRepository.save(credentials);
                                log.info("[OAuth] Automatically saved Instagram credentials for @{} to the database.", igUsername);
                                resultHtml.append(" - <span style='color: green;'>Saved & Activated!</span>");
                            } else {
                                resultHtml.append(" - <span style='color: gray;'>Skipped (Non-target profile)</span>");
                            }
                        } else {
                            resultHtml.append(" - <span style='color: red;'>No linked Instagram Professional account found.</span>");
                        }
                        resultHtml.append("</li>");
                    }
                    resultHtml.append("</ul><p>Meshada's backend is now fully configured to post and reply to Instagram Reels automatically. You can close this window.</p>");

                    return ResponseEntity.ok(resultHtml.toString());
                })
                .onErrorResume(e -> {
                    log.error("[OAuth] Failed to complete token exchange flow: {}", e.getMessage(), e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("<h3>Authentication Error</h3><p>Failed to exchange OAuth token: " + e.getMessage() + "</p>"));
                });
    }

    /**
     * Dynamically construct the redirect URI using the incoming request's host/port.
     */
    private String buildRedirectUri(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getHeader("Host"); // captures port if necessary
        
        // Handle Railway proxy headers (X-Forwarded-Proto and X-Forwarded-Host)
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto != null) {
            scheme = forwardedProto;
        }
        
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (forwardedHost != null) {
            host = forwardedHost;
        }

        return scheme + "://" + host + "/api/v1/social/auth/instagram/callback";
    }
}
