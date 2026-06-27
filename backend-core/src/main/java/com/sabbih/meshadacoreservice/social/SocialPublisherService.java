package com.sabbih.meshadacoreservice.social;

import com.sabbih.meshadacoreservice.ugc.FashionNewsPost;
import com.sabbih.meshadacoreservice.ugc.UGCVideo;
import com.sabbih.meshadacoreservice.ugc.UGCVideoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Service
@Slf4j
public class SocialPublisherService {

    private final WebClient webClient;
    private final SocialCredentialsRepository credentialsRepository;
    private final UGCVideoRepository videoRepository;

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

    @Value("${meshada.app.url:https://www.meshada.com}")
    private String appUrl;

    public SocialPublisherService(WebClient.Builder webClientBuilder, 
                                 SocialCredentialsRepository credentialsRepository,
                                 UGCVideoRepository videoRepository) {
        this.webClient = webClientBuilder.build();
        this.credentialsRepository = credentialsRepository;
        this.videoRepository = videoRepository;
    }

    private String resolveAbsoluteUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        String cleanUrl = url.startsWith("/") ? url.substring(1) : url;
        String cleanBase = appUrl.endsWith("/") ? appUrl : appUrl + "/";
        return cleanBase + cleanUrl;
    }

    /**
     * Publish a newly generated UGC Video to Instagram Reels, TikTok, and Twitter/X.
     * Running asynchronously so it doesn't block the video generation response.
     */
    @Async
    public void publishVideoToSocial(UGCVideo video) {
        String instagramCaption = String.format("Obsessed with this %s! 💅 Styled by AI Model %s. Comment \"FIT\" and I'll DM you the link!",
                video.getItemName(), video.getModelName());
        
        String twitterCaption = String.format("Obsessed with this %s! 💅 Styled by AI Model %s. Shop the look here: %s",
                video.getItemName(), video.getModelName(), video.getAffiliateLink());

        String tiktokCaption = String.format("Obsessed with this %s! 💅 Styled by AI Model %s. Link in bio!",
                video.getItemName(), video.getModelName());

        String pinterestDescription = String.format("Obsessed with this %s! Styled by AI Model %s.",
                video.getItemName(), video.getModelName());
        
        log.info("[Social Publisher] Starting auto-publishing workflow for video ID: {} ({})", 
                video.getId(), video.getItemName());

        String absoluteVideoUrl = resolveAbsoluteUrl(video.getUrl());
        String absoluteVtonImageUrl = resolveAbsoluteUrl(video.getVtonImageUrl());

        String instagramPostId = null;
        boolean pinterestSuccess = false;

        // 1. Post to Instagram Reels
        try {
            instagramPostId = publishToInstagramReels(absoluteVideoUrl, instagramCaption);
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.error("[Social Publisher] Failed to publish to Instagram: {} - Response: {}", e.getMessage(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[Social Publisher] Failed to publish to Instagram: {}", e.getMessage());
        }

        // 2. Post to Twitter/X
        if (absoluteVideoUrl != null && !absoluteVideoUrl.isEmpty()) {
            try {
                publishToTwitter(absoluteVideoUrl, twitterCaption);
            } catch (Exception e) {
                log.error("[Social Publisher] Failed to publish to Twitter/X: {}", e.getMessage());
            }
        } else {
            log.warn("[Twitter Publisher] Video URL is empty. Twitter publishing skipped.");
        }
 
        // 3. Post to TikTok
        if (absoluteVideoUrl != null && !absoluteVideoUrl.isEmpty()) {
            try {
                publishToTikTok(absoluteVideoUrl, tiktokCaption);
            } catch (Exception e) {
                log.error("[Social Publisher] Failed to publish to TikTok: {}", e.getMessage());
            }
        } else {
            log.warn("[TikTok Publisher] Video URL is empty. TikTok publishing skipped.");
        }
 
        // 4. Post to Pinterest
        try {
            pinterestSuccess = publishToPinterest(absoluteVideoUrl, video.getItemName(), pinterestDescription, video.getAffiliateLink(), absoluteVtonImageUrl);
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.error("[Social Publisher] Failed to publish to Pinterest: {} - Response: {}", e.getMessage(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[Social Publisher] Failed to publish to Pinterest: {}", e.getMessage());
        }
 
        // 5. Post to Facebook Page
        try {
            publishToFacebookPage(absoluteVideoUrl, instagramCaption);
        } catch (Exception e) {
            log.error("[Social Publisher] Failed to publish to Facebook Page: {}", e.getMessage());
        }
 
        // Save publication state if at least one main platform succeeded
        if (instagramPostId != null || pinterestSuccess) {
            video.setPublished(true);
            if (instagramPostId != null) {
                video.setInstagramPostId(instagramPostId);
            }
            videoRepository.save(video);
            log.info("[Social Publisher] Successfully marked video ID: {} as published in database.", video.getId());
        }
    }
 
    /**
     * Publish video as Instagram Reels.
     * Meta API Reels flow:
     * 1. POST /v19.0/{businessAccountId}/media?media_type=REELS&video_url={videoUrl}&caption={caption}
     * 2. Poll /v19.0/{creationId} for status_code == FINISHED
     * 3. POST /v19.0/{businessAccountId}/media_publish?creation_id={creationId}
     */
    private String publishToInstagramReels(String videoUrl, String caption) {
        String pageToken = instagramPageAccessToken;
        String bizAccountId = instagramBusinessAccountId;
 
        // Dynamic lookup from database
        java.util.Optional<SocialCredentials> credsOpt = credentialsRepository.findById("instagram");
        if (credsOpt.isPresent()) {
            SocialCredentials creds = credsOpt.get();
            if (creds.getAccessToken() != null && !creds.getAccessToken().isEmpty()) {
                pageToken = creds.getAccessToken();
            }
            if (creds.getBusinessAccountId() != null && !creds.getBusinessAccountId().isEmpty()) {
                bizAccountId = creds.getBusinessAccountId();
            }
        }
 
        if (pageToken == null || pageToken.isEmpty() ||
                bizAccountId == null || bizAccountId.isEmpty()) {
            log.warn("[Instagram Publisher] Credentials not configured. Reels publishing skipped.");
            return null;
        }
 
        if (videoUrl == null || videoUrl.isEmpty()) {
            log.warn("[Instagram Publisher] Video URL is empty. Reels publishing skipped.");
            return null;
        }
 
        log.info("[Instagram Publisher] Creating Reels media container for business account: {} with video URL: {}", bizAccountId, videoUrl);
        String mediaUrl = "https://graph.facebook.com/v19.0/" + bizAccountId + "/media";
        final String finalPageToken = pageToken;
 
        Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("media_type", "REELS");
        requestBody.put("video_url", videoUrl);
        requestBody.put("caption", caption);
        requestBody.put("access_token", finalPageToken);
 
        Map response = webClient.post()
                .uri(mediaUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
 
        if (response == null || !response.containsKey("id")) {
            log.error("[Instagram Publisher] Failed to create Reels container. Response: {}", response);
            return null;
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
                return null;
            }
 
            URI checkUri = UriComponentsBuilder.fromHttpUrl(statusCheckUrl)
                    .queryParam("fields", "status_code")
                    .queryParam("access_token", finalPageToken)
                    .build()
                    .toUri();
 
            Map statusResponse = webClient.get()
                    .uri(checkUri)
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
                    return null;
                }
            }
            attempts++;
        }
 
        if (!finished) {
            log.error("[Instagram Publisher] Reels processing timed out on Meta's server.");
            return null;
        }
 
        log.info("[Instagram Publisher] Container ready. Publishing Reels...");
        String publishUrl = "https://graph.facebook.com/v19.0/" + bizAccountId + "/media_publish";
 
        Map<String, Object> publishBody = new java.util.HashMap<>();
        publishBody.put("creation_id", creationId);
        publishBody.put("access_token", finalPageToken);
 
        Map publishResponse = webClient.post()
                .uri(publishUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(publishBody)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
 
        if (publishResponse != null && publishResponse.containsKey("id")) {
            String postId = publishResponse.get("id").toString();
            log.info("[Instagram Publisher] Reels published successfully! Post ID: {}", postId);
            return postId;
        } else {
            log.error("[Instagram Publisher] Failed to publish Reels: {}", publishResponse);
            return null;
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

        Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("text", (caption != null ? caption : "Check out this outfit!") + "\n\nWatch: " + (videoUrl != null ? videoUrl : ""));

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

        Map<String, Object> postInfo = new java.util.HashMap<>();
        postInfo.put("title", caption != null ? caption : "New Outfit");
        postInfo.put("privacy_level", "PUBLIC_TO_EVERYONE");
        postInfo.put("video_cover_timestamp_ms", 1000);

        Map<String, Object> sourceInfo = new java.util.HashMap<>();
        sourceInfo.put("source", "PULL_FROM_URL");
        sourceInfo.put("video_url", videoUrl != null ? videoUrl : "");

        Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("post_info", postInfo);
        requestBody.put("source_info", sourceInfo);

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
    private boolean publishToPinterest(String mediaUrl, String title, String description, String link, String coverImageUrl) {
        String token = pinterestAccessToken;
        String boardId = pinterestBoardId;

        // Dynamic lookup from database
        java.util.Optional<SocialCredentials> credsOpt = credentialsRepository.findById("pinterest");
        if (credsOpt.isPresent()) {
            SocialCredentials creds = credsOpt.get();
            if (creds.getAccessToken() != null && !creds.getAccessToken().isEmpty()) {
                token = creds.getAccessToken();
            }
            if (creds.getBusinessAccountId() != null && !creds.getBusinessAccountId().isEmpty()) {
                boardId = creds.getBusinessAccountId();
            }
        }

        if (token == null || token.isEmpty() ||
                boardId == null || boardId.isEmpty()) {
            log.warn("[Pinterest Publisher] Access Token or Board ID not configured. Pinterest publishing skipped.");
            return false;
        }
 
        String url = "https://api.pinterest.com/v5/pins";
 
        // Since Pinterest requires direct video file binary upload to S3 for video Pins,
        // we publish a high-quality visual Image Pin showcasing the virtual try-on model.
        Map<String, Object> mediaSource = new java.util.HashMap<>();
        mediaSource.put("source_type", "image_url");
        String finalUrl = (coverImageUrl != null && !coverImageUrl.isEmpty()) ? coverImageUrl : mediaUrl;
        if (finalUrl == null || finalUrl.isEmpty()) {
            finalUrl = "https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=800"; // fallback
        }
        mediaSource.put("url", finalUrl);
        log.info("[Pinterest Publisher] Creating Pin on board: {} using media URL: {}", boardId, finalUrl);
 
        Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("board_id", boardId);
        requestBody.put("title", title != null ? title : "Styled by Meshada");
        if (description != null) {
            requestBody.put("description", description);
        }
        if (link != null) {
            requestBody.put("link", link);
        }
        requestBody.put("media_source", mediaSource);
 
        try {
            Map response = webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
 
            if (response != null && response.containsKey("id")) {
                log.info("[Pinterest Publisher] Pin created successfully! Pin ID: {}", response.get("id"));
                return true;
            } else {
                log.error("[Pinterest Publisher] Failed to create Pin: {}", response);
                return false;
            }
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.error("[Pinterest Publisher] Failed to create Pin: {} - Response: {}", e.getMessage(), e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.error("[Pinterest Publisher] Failed to create Pin: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Publish a News Post infographic to social media channels,
     * including posting a follow-up comment with the monetized link.
     */
    public void publishNewsPostBridge(FashionNewsPost post) {
        String instagramCaption = String.format("%s 💅 Read the full details via the link below!", post.getSocialCaption());
        String twitterCaption = String.format("%s\n\nRead more: %s", post.getTitle(), post.getMonetizedUrl());
        String pinterestDescription = String.format("%s. Read full details: %s", post.getSocialCaption(), post.getMonetizedUrl());

        log.info("[Social Publisher] Starting news post publishing for ID: {}", post.getId());

        String[] urls = post.getImageUrls() != null ? post.getImageUrls().split(",") : new String[0];
        java.util.List<String> absoluteUrls = new java.util.ArrayList<>();
        for (String url : urls) {
            absoluteUrls.add(resolveAbsoluteUrl(url));
        }

        if (absoluteUrls.isEmpty()) {
            log.warn("[Social Publisher] No image URLs found for News Post: {}", post.getId());
            return;
        }

        String mainCoverUrl = absoluteUrls.get(0);

        // 1. Post to Instagram Carousel Feed
        String instagramPostId = null;
        try {
            if (absoluteUrls.size() > 1) {
                instagramPostId = publishToInstagramCarousel(absoluteUrls, instagramCaption);
            } else {
                instagramPostId = publishToInstagramImage(mainCoverUrl, instagramCaption);
            }
            
            if (instagramPostId != null) {
                post.setInstagramPostId(instagramPostId);
                // Post follow-up comment with the monetized news URL
                String commentMsg = "Read more here: " + post.getMonetizedUrl();
                commentOnInstagramMedia(instagramPostId, commentMsg);
            }
        } catch (Exception e) {
            log.error("[Social Publisher] Failed to publish news image to Instagram: {}", e.getMessage());
        }

        // 2. Post to Twitter/X
        try {
            publishToTwitter(null, twitterCaption); 
        } catch (Exception e) {
            log.error("[Social Publisher] Failed to post Tweet: {}", e.getMessage());
        }

        // 3. Post to Pinterest
        try {
            boolean pinSuccess = publishToPinterest(mainCoverUrl, post.getTitle(), pinterestDescription, post.getMonetizedUrl(), mainCoverUrl);
            if (pinSuccess) {
                log.info("[Social Publisher] Successfully posted to Pinterest.");
            }
        } catch (Exception e) {
            log.error("[Social Publisher] Failed to post to Pinterest: {}", e.getMessage());
        }

        // 4. Post to Facebook Page
        try {
            publishImageToFacebookPage(mainCoverUrl, instagramCaption);
        } catch (Exception e) {
            log.error("[Social Publisher] Failed to post image to Facebook Page: {}", e.getMessage());
        }
    }

    private String publishToInstagramCarousel(java.util.List<String> imageUrls, String caption) {
        String pageToken = instagramPageAccessToken;
        String bizAccountId = instagramBusinessAccountId;

        java.util.Optional<SocialCredentials> credsOpt = credentialsRepository.findById("instagram");
        if (credsOpt.isPresent()) {
            SocialCredentials creds = credsOpt.get();
            if (creds.getAccessToken() != null && !creds.getAccessToken().isEmpty()) {
                pageToken = creds.getAccessToken();
            }
            if (creds.getBusinessAccountId() != null && !creds.getBusinessAccountId().isEmpty()) {
                bizAccountId = creds.getBusinessAccountId();
            }
        }

        if (pageToken == null || pageToken.isEmpty() ||
                bizAccountId == null || bizAccountId.isEmpty()) {
            log.warn("[Instagram Publisher] Credentials not configured. Carousel publishing skipped.");
            return null;
        }

        log.info("[Instagram Publisher] Creating carousel item containers for biz account: {}", bizAccountId);
        String mediaUrl = "https://graph.facebook.com/v19.0/" + bizAccountId + "/media";
        final String finalPageToken = pageToken;
        
        java.util.List<String> itemIds = new java.util.ArrayList<>();
        for (String url : imageUrls) {
            Map<String, Object> itemBody = new java.util.HashMap<>();
            itemBody.put("image_url", url);
            itemBody.put("is_carousel_item", true);
            itemBody.put("access_token", finalPageToken);
            
            try {
                Map itemResponse = webClient.post()
                        .uri(mediaUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(itemBody)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();
                        
                if (itemResponse != null && itemResponse.containsKey("id")) {
                    itemIds.add(itemResponse.get("id").toString());
                } else {
                    log.error("[Instagram Publisher] Failed to create item container for URL {}: {}", url, itemResponse);
                }
            } catch (Exception e) {
                log.error("[Instagram Publisher] Exception creating item container: {}", e.getMessage());
            }
        }

        if (itemIds.size() < 2) {
            log.error("[Instagram Publisher] Not enough valid carousel item containers (needed at least 2, got {}).", itemIds.size());
            return null;
        }

        log.info("[Instagram Publisher] Creating Carousel container with {} items...", itemIds.size());
        Map<String, Object> carouselBody = new java.util.HashMap<>();
        carouselBody.put("media_type", "CAROUSEL");
        carouselBody.put("children", itemIds);
        carouselBody.put("caption", caption);
        carouselBody.put("access_token", finalPageToken);

        Map response = webClient.post()
                .uri(mediaUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(carouselBody)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("id")) {
            log.error("[Instagram Publisher] Failed to create Carousel container. Response: {}", response);
            return null;
        }

        String creationId = response.get("id").toString();
        log.info("[Instagram Publisher] Carousel container created. ID: {}. Publishing...", creationId);

        String publishUrl = "https://graph.facebook.com/v19.0/" + bizAccountId + "/media_publish";
        Map<String, Object> publishBody = new java.util.HashMap<>();
        publishBody.put("creation_id", creationId);
        publishBody.put("access_token", finalPageToken);

        Map publishResponse = webClient.post()
                .uri(publishUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(publishBody)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (publishResponse != null && publishResponse.containsKey("id")) {
            String postId = publishResponse.get("id").toString();
            log.info("[Instagram Publisher] Carousel published successfully! Post ID: {}", postId);
            return postId;
        } else {
            log.error("[Instagram Publisher] Failed to publish Carousel: {}", publishResponse);
            return null;
        }
    }


    private String publishToInstagramImage(String imageUrl, String caption) {
        String pageToken = instagramPageAccessToken;
        String bizAccountId = instagramBusinessAccountId;

        java.util.Optional<SocialCredentials> credsOpt = credentialsRepository.findById("instagram");
        if (credsOpt.isPresent()) {
            SocialCredentials creds = credsOpt.get();
            if (creds.getAccessToken() != null && !creds.getAccessToken().isEmpty()) {
                pageToken = creds.getAccessToken();
            }
            if (creds.getBusinessAccountId() != null && !creds.getBusinessAccountId().isEmpty()) {
                bizAccountId = creds.getBusinessAccountId();
            }
        }

        if (pageToken == null || pageToken.isEmpty() ||
                bizAccountId == null || bizAccountId.isEmpty()) {
            log.warn("[Instagram Publisher] Credentials not configured. Image publishing skipped.");
            return null;
        }

        if (imageUrl == null || imageUrl.isEmpty()) {
            log.warn("[Instagram Publisher] Image URL is empty. Image publishing skipped.");
            return null;
        }

        log.info("[Instagram Publisher] Creating Image media container with image URL: {}", imageUrl);
        String mediaUrl = "https://graph.facebook.com/v19.0/" + bizAccountId + "/media";
        final String finalPageToken = pageToken;

        Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("image_url", imageUrl);
        requestBody.put("caption", caption);
        requestBody.put("access_token", finalPageToken);

        Map response = webClient.post()
                .uri(mediaUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("id")) {
            log.error("[Instagram Publisher] Failed to create Image container. Response: {}", response);
            return null;
        }

        String creationId = response.get("id").toString();
        log.info("[Instagram Publisher] Image container created. ID: {}. Publishing...", creationId);

        String publishUrl = "https://graph.facebook.com/v19.0/" + bizAccountId + "/media_publish";
        Map<String, Object> publishBody = new java.util.HashMap<>();
        publishBody.put("creation_id", creationId);
        publishBody.put("access_token", finalPageToken);

        Map publishResponse = webClient.post()
                .uri(publishUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(publishBody)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (publishResponse != null && publishResponse.containsKey("id")) {
            String postId = publishResponse.get("id").toString();
            log.info("[Instagram Publisher] Image published successfully! Post ID: {}", postId);
            return postId;
        } else {
            log.error("[Instagram Publisher] Failed to publish Image: {}", publishResponse);
            return null;
        }
    }

    public boolean commentOnInstagramMedia(String mediaId, String message) {
        String pageToken = instagramPageAccessToken;
        java.util.Optional<SocialCredentials> credsOpt = credentialsRepository.findById("instagram");
        if (credsOpt.isPresent() && credsOpt.get().getAccessToken() != null && !credsOpt.get().getAccessToken().isEmpty()) {
            pageToken = credsOpt.get().getAccessToken();
        }

        if (pageToken == null || pageToken.isEmpty()) {
            log.warn("[Instagram Client] Page Access Token not configured. Comment logged: {}", message);
            return false;
        }

        try {
            log.info("[Instagram Client] Posting comment to media ID: {}", mediaId);
            String url = "https://graph.facebook.com/v19.0/" + mediaId + "/comments?message={message}&access_token={token}";
            final String finalPageToken = pageToken;

            Map response = webClient.post()
                    .uri(url, message, finalPageToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("id")) {
                log.info("[Instagram Client] Comment posted successfully. Comment ID: {}", response.get("id"));
                return true;
            }
            log.warn("[Instagram Client] Received unexpected comment response: {}", response);
            return false;
        } catch (Exception e) {
            log.error("[Instagram Client] Failed to post comment: {}", e.getMessage(), e);
            return false;
        }
    }

    private void publishToFacebookPage(String videoUrl, String caption) {
        String pageToken = null;
        String pageId = null;

        java.util.Optional<SocialCredentials> credsOpt = credentialsRepository.findById("facebook");
        if (credsOpt.isPresent()) {
            SocialCredentials creds = credsOpt.get();
            pageToken = creds.getAccessToken();
            pageId = creds.getBusinessAccountId();
        }

        if (pageToken == null || pageToken.isEmpty() || pageId == null || pageId.isEmpty()) {
            log.warn("[Facebook Publisher] Credentials not configured in database for 'facebook'. Skipping Facebook publishing.");
            return;
        }

        log.info("[Facebook Publisher] Publishing video to Facebook Page ID: {}", pageId);
        String url = "https://graph.facebook.com/v19.0/" + pageId + "/videos";

        Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("file_url", videoUrl);
        requestBody.put("description", caption);
        requestBody.put("access_token", pageToken);

        try {
            Map response = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("id")) {
                log.info("[Facebook Publisher] Video published successfully to Facebook Page! Video ID: {}", response.get("id"));
            } else {
                log.error("[Facebook Publisher] Failed to publish video to Facebook Page: {}", response);
            }
        } catch (Exception e) {
            log.error("[Facebook Publisher] Exception publishing to Facebook Page: {}", e.getMessage(), e);
        }
    }

    private void publishImageToFacebookPage(String imageUrl, String caption) {
        String pageToken = null;
        String pageId = null;

        java.util.Optional<SocialCredentials> credsOpt = credentialsRepository.findById("facebook");
        if (credsOpt.isPresent()) {
            SocialCredentials creds = credsOpt.get();
            pageToken = creds.getAccessToken();
            pageId = creds.getBusinessAccountId();
        }

        if (pageToken == null || pageToken.isEmpty() || pageId == null || pageId.isEmpty()) {
            log.warn("[Facebook Publisher] Credentials not configured in database for 'facebook'. Skipping Facebook image publishing.");
            return;
        }

        log.info("[Facebook Publisher] Publishing image to Facebook Page ID: {}", pageId);
        String url = "https://graph.facebook.com/v19.0/" + pageId + "/photos";

        Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("url", imageUrl);
        requestBody.put("message", caption);
        requestBody.put("access_token", pageToken);

        try {
            Map response = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("id")) {
                log.info("[Facebook Publisher] Image published successfully to Facebook Page! Photo ID: {}", response.get("id"));
            } else {
                log.error("[Facebook Publisher] Failed to publish image to Facebook Page: {}", response);
            }
        } catch (Exception e) {
            log.error("[Facebook Publisher] Exception publishing image to Facebook Page: {}", e.getMessage(), e);
        }
    }
}

