package com.sabbih.meshadacoreservice.social;

import com.sabbih.meshadacoreservice.ugc.UGCVideo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@Slf4j
public class SocialPublisherService {

    private final WebClient webClient;

    @Value("${meshada.social.instagram.pageAccessToken:}")
    private String instagramPageAccessToken;

    @Value("${meshada.social.instagram.businessAccountId:}")
    private String instagramBusinessAccountId;

    @Value("${meshada.social.twitter.bearerToken:}")
    private String twitterBearerToken;

    @Value("${meshada.social.tiktok.accessToken:}")
    private String tiktokAccessToken;

    @Value("${meshada.social.pinterest.accessToken:}")
    private String pinterestAccessToken;

    @Value("${meshada.social.pinterest.boardId:}")
    private String pinterestBoardId;

    public SocialPublisherService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Publish a newly generated UGC Video to Instagram Reels, TikTok, and Twitter/X.
     * Running asynchronously so it doesn't block the video generation response.
     */
    @Async
    public void publishVideoToSocial(UGCVideo video) {
        String caption = String.format("Obsessed with this %s! 💅 Styled by AI Model %s. Shop the look here: %s",
                video.getItemName(), video.getModelName(), video.getAffiliateLink());
        
        log.info("[Social Publisher] Starting auto-publishing workflow for video ID: {} ({})", 
                video.getId(), video.getItemName());

        // 1. Post to Instagram Reels
        try {
            publishToInstagramReels(video.getUrl(), caption);
        } catch (Exception e) {
            log.error("[Social Publisher] Failed to publish to Instagram: {}", e.getMessage());
        }

        // 2. Post to Twitter/X
        try {
            publishToTwitter(video.getUrl(), caption);
        } catch (Exception e) {
            log.error("[Social Publisher] Failed to publish to Twitter/X: {}", e.getMessage());
        }

        // 3. Post to TikTok
        try {
            publishToTikTok(video.getUrl(), caption);
        } catch (Exception e) {
            log.error("[Social Publisher] Failed to publish to TikTok: {}", e.getMessage());
        }

        // 4. Post to Pinterest
        try {
            publishToPinterest(video.getUrl(), video.getItemName(), caption, video.getAffiliateLink(), video.getVtonImageUrl());
        } catch (Exception e) {
            log.error("[Social Publisher] Failed to publish to Pinterest: {}", e.getMessage());
        }
    }

    /**
     * Publish video as Instagram Reels.
     * Meta API Reels flow:
     * 1. POST /v19.0/{businessAccountId}/media?media_type=REELS&video_url={videoUrl}&caption={caption}
     * 2. Poll /v19.0/{creationId} for status_code == FINISHED
     * 3. POST /v19.0/{businessAccountId}/media_publish?creation_id={creationId}
     */
    private void publishToInstagramReels(String videoUrl, String caption) {
        if (instagramPageAccessToken == null || instagramPageAccessToken.isEmpty() ||
                instagramBusinessAccountId == null || instagramBusinessAccountId.isEmpty()) {
            log.warn("[Instagram Publisher] Credentials not configured. Reels publishing skipped.");
            return;
        }

        log.info("[Instagram Publisher] Creating Reels media container...");
        String mediaUrl = "https://graph.facebook.com/v19.0/" + instagramBusinessAccountId + "/media";

        Map response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(mediaUrl)
                        .queryParam("media_type", "REELS")
                        .queryParam("video_url", videoUrl)
                        .queryParam("caption", caption)
                        .queryParam("access_token", instagramPageAccessToken)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("id")) {
            log.error("[Instagram Publisher] Failed to create Reels container. Response: {}", response);
            return;
        }

        String creationId = response.get("id").toString();
        log.info("[Instagram Publisher] Reels container created. ID: {}. Polling processing status...", creationId);

        // Poll container status
        boolean finished = false;
        int attempts = 0;
        String statusCheckUrl = "https://graph.facebook.com/v19.0/" + creationId;

        while (attempts < 15 && !finished) {
            try {
                Thread.sleep(10000); // Wait 10 seconds between checks
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }

            Map statusResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(statusCheckUrl)
                            .queryParam("fields", "status_code")
                            .queryParam("access_token", instagramPageAccessToken)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (statusResponse != null && statusResponse.containsKey("status_code")) {
                String statusCode = statusResponse.get("status_code").toString();
                log.info("[Instagram Publisher] Status check attempt {}/15: {}", attempts + 1, statusCode);
                if ("FINISHED".equalsIgnoreCase(statusCode)) {
                    finished = true;
                } else if ("ERROR".equalsIgnoreCase(statusCode)) {
                    log.error("[Instagram Publisher] Reels processing finished with ERROR: {}", statusResponse);
                    return;
                }
            }
            attempts++;
        }

        if (!finished) {
            log.error("[Instagram Publisher] Reels processing timed out on Meta's server.");
            return;
        }

        log.info("[Instagram Publisher] Container ready. Publishing Reels...");
        String publishUrl = "https://graph.facebook.com/v19.0/" + instagramBusinessAccountId + "/media_publish";

        Map publishResponse = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(publishUrl)
                        .queryParam("creation_id", creationId)
                        .queryParam("access_token", instagramPageAccessToken)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (publishResponse != null && publishResponse.containsKey("id")) {
            log.info("[Instagram Publisher] Reels published successfully! Post ID: {}", publishResponse.get("id"));
        } else {
            log.error("[Instagram Publisher] Failed to publish Reels: {}", publishResponse);
        }
    }

    /**
     * Publish Tweet with Video details on Twitter/X.
     * Twitter API v2: POST /2/tweets
     */
    private void publishToTwitter(String videoUrl, String caption) {
        if (twitterBearerToken == null || twitterBearerToken.isEmpty()) {
            log.warn("[Twitter Publisher] Bearer Token not configured. Twitter publishing skipped.");
            return;
        }

        log.info("[Twitter Publisher] Creating Tweet...");
        String url = "https://api.twitter.com/2/tweets";

        Map<String, Object> requestBody = Map.of(
                "text", caption + "\n\nWatch: " + videoUrl
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
            log.info("[Twitter Publisher] Tweet posted successfully.");
        } else {
            log.error("[Twitter Publisher] Failed to post Tweet: {}", response);
        }
    }

    /**
     * Publish video on TikTok.
     * TikTok Direct Post API: POST /v2/post/publish/video/init/
     */
    private void publishToTikTok(String videoUrl, String caption) {
        if (tiktokAccessToken == null || tiktokAccessToken.isEmpty()) {
            log.warn("[TikTok Publisher] Access Token not configured. TikTok publishing skipped.");
            return;
        }

        log.info("[TikTok Publisher] Initiating video publish request...");
        String url = "https://open.tiktokapis.com/v2/post/publish/video/init/";

        Map<String, Object> postInfo = Map.of(
                "title", caption,
                "privacy_level", "PUBLIC_TO_EVERYONE",
                "video_cover_timestamp_ms", 1000
        );

        Map<String, Object> sourceInfo = Map.of(
                "source", "PULL_FROM_URL",
                "video_url", videoUrl
        );

        Map<String, Object> requestBody = Map.of(
                "post_info", postInfo,
                "source_info", sourceInfo
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
            log.info("[TikTok Publisher] TikTok video upload triggered successfully.");
        } else {
            log.error("[TikTok Publisher] Failed to upload TikTok video: {}", response);
        }
    }

    /**
     * Publish Pin on Pinterest.
     * Pinterest API v5: POST https://api.pinterest.com/v5/pins
     */
    private void publishToPinterest(String mediaUrl, String title, String description, String link, String coverImageUrl) {
        if (pinterestAccessToken == null || pinterestAccessToken.isEmpty() ||
                pinterestBoardId == null || pinterestBoardId.isEmpty()) {
            log.warn("[Pinterest Publisher] Access Token or Board ID not configured. Pinterest publishing skipped.");
            return;
        }

        log.info("[Pinterest Publisher] Creating Pin on board: {}", pinterestBoardId);
        String url = "https://api.pinterest.com/v5/pins";

        // Since Pinterest requires direct video file binary upload to S3 for video Pins (which is highly complex asynchronously),
        // we publish a high-quality visual Image Pin showcasing the virtual try-on model wearing the apparel.
        // This links directly to the affiliate shop URL, which is the most standard Pinterest affiliate flow.
        Map<String, Object> mediaSource = Map.of(
                "source_type", "image_url",
                "url", coverImageUrl != null && !coverImageUrl.isEmpty() ? coverImageUrl : mediaUrl
        );

        Map<String, Object> requestBody = Map.of(
                "board_id", pinterestBoardId,
                "title", title != null ? title : "Styled by Meshada",
                "description", description,
                "link", link,
                "media_source", mediaSource
        );

        try {
            Map response = webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + pinterestAccessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("id")) {
                log.info("[Pinterest Publisher] Pin created successfully! Pin ID: {}", response.get("id"));
            } else {
                log.error("[Pinterest Publisher] Failed to create Pin: {}", response);
            }
        } catch (Exception e) {
            log.error("[Pinterest Publisher] Failed to create Pin: {}", e.getMessage(), e);
        }
    }
}
