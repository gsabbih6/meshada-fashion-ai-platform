package com.sabbih.meshadacoreservice.social;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@Slf4j
public class SocialPlatformClient {

    private final WebClient webClient;
    private final SocialCredentialsRepository credentialsRepository;

    @Value("${meshada.social.instagram.pageAccessToken:}")
    private String instagramPageAccessToken;

    @Value("${meshada.social.instagram.businessAccountId:}")
    private String instagramBusinessAccountId;

    @Value("${meshada.social.twitter.bearerToken:}")
    private String twitterBearerToken;

    @Value("${meshada.social.tiktok.accessToken:}")
    private String tiktokAccessToken;

    public SocialPlatformClient(WebClient.Builder webClientBuilder, SocialCredentialsRepository credentialsRepository) {
        this.webClient = webClientBuilder.build();
        this.credentialsRepository = credentialsRepository;
    }

    /**
     * Reply to an Instagram Comment.
     * Meta Graph API: POST /v19.0/{comment-id}/replies
     */
    public boolean replyToInstagramComment(String commentId, String message) {
        String pageToken = instagramPageAccessToken;
        java.util.Optional<SocialCredentials> credsOpt = credentialsRepository.findById("instagram");
        if (credsOpt.isPresent() && credsOpt.get().getAccessToken() != null && !credsOpt.get().getAccessToken().isEmpty()) {
            pageToken = credsOpt.get().getAccessToken();
        }

        if (pageToken == null || pageToken.isEmpty()) {
            log.warn("[Instagram Client] Page Access Token not configured. Reply logged: {}", message);
            return false;
        }

        try {
            log.info("[Instagram Client] Sending auto-reply to comment ID: {}", commentId);
            String url = "https://graph.facebook.com/v19.0/" + commentId + "/replies";
            final String finalPageToken = pageToken;

            Map<String, String> response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(url)
                            .queryParam("message", message)
                            .queryParam("access_token", finalPageToken)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("id")) {
                log.info("[Instagram Client] Reply posted successfully. Reply ID: {}", response.get("id"));
                return true;
            }
            log.warn("[Instagram Client] Received unexpected reply response: {}", response);
            return false;

        } catch (Exception e) {
            log.error("[Instagram Client] Failed to reply to comment: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send a private reply DM to an Instagram Comment.
     * Meta Graph API: POST /v19.0/{comment-id}/private_replies
     */
    public boolean sendPrivateDMToInstagramComment(String commentId, String message) {
        String pageToken = instagramPageAccessToken;
        java.util.Optional<SocialCredentials> credsOpt = credentialsRepository.findById("instagram");
        if (credsOpt.isPresent() && credsOpt.get().getAccessToken() != null && !credsOpt.get().getAccessToken().isEmpty()) {
            pageToken = credsOpt.get().getAccessToken();
        }

        if (pageToken == null || pageToken.isEmpty()) {
            log.warn("[Instagram Client] Page Access Token not configured. Private DM logged: {}", message);
            return false;
        }

        try {
            log.info("[Instagram Client] Sending private DM to comment ID: {}", commentId);
            String url = "https://graph.facebook.com/v19.0/" + commentId + "/private_replies";
            final String finalPageToken = pageToken;

            Map response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(url)
                            .queryParam("message", message)
                            .queryParam("access_token", finalPageToken)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("id")) {
                log.info("[Instagram Client] Private DM sent successfully. Response ID: {}", response.get("id"));
                return true;
            }
            log.warn("[Instagram Client] Received unexpected private DM response: {}", response);
            return false;

        } catch (Exception e) {
            log.error("[Instagram Client] Failed to send private DM: {}", e.getMessage(), e);
            return false;
        }
    }

    /**

     * Reply to a Twitter/X Tweet.
     * Twitter API v2: POST /2/tweets
     */
    public boolean replyToTwitterComment(String tweetId, String username, String message) {
        if (twitterBearerToken == null || twitterBearerToken.isEmpty()) {
            log.warn("[Twitter Client] Bearer Token not configured. Reply logged: @{} {}", username, message);
            return false;
        }

        try {
            log.info("[Twitter Client] Replying to Tweet ID: {}", tweetId);
            String url = "https://api.twitter.com/2/tweets";

            Map<String, Object> replyConfig = Map.of("in_reply_to_tweet_id", tweetId);
            Map<String, Object> requestBody = Map.of(
                    "text", "@" + username + " " + message,
                    "reply", replyConfig
            );

            Map response = webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + twitterBearerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("data")) {
                log.info("[Twitter Client] Tweet reply posted successfully.");
                return true;
            }
            log.warn("[Twitter Client] Received unexpected response: {}", response);
            return false;

        } catch (Exception e) {
            log.error("[Twitter Client] Failed to reply to Tweet: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Reply to a TikTok Comment.
     * TikTok Open API: POST /v2/comment/reply/
     */
    public boolean replyToTikTokComment(String commentId, String message) {
        if (tiktokAccessToken == null || tiktokAccessToken.isEmpty()) {
            log.warn("[TikTok Client] Access Token not configured. Reply logged: {}", message);
            return false;
        }

        try {
            log.info("[TikTok Client] Replying to TikTok comment ID: {}", commentId);
            String url = "https://open.tiktokapis.com/v2/comment/reply/";

            Map<String, Object> requestBody = Map.of(
                    "comment_id", commentId,
                    "text", message
            );

            Map response = webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tiktokAccessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("data")) {
                log.info("[TikTok Client] TikTok reply posted successfully.");
                return true;
            }
            log.warn("[TikTok Client] Received unexpected response: {}", response);
            return false;

        } catch (Exception e) {
            log.error("[TikTok Client] Failed to reply to TikTok comment: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Pinterest does not support programmatically replying to comments on Pins via API v5.
     */
    public boolean replyToPinterestComment(String commentId, String message) {
        log.error("[Pinterest Client] Pinterest API v5 does not support replying to Pin comments programmatically.");
        return false;
    }
}
