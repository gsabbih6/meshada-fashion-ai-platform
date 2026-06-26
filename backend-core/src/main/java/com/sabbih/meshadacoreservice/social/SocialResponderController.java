package com.sabbih.meshadacoreservice.social;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/social")
public class SocialResponderController {

    private final SocialResponderService responderService;

    @Autowired
    public SocialResponderController(SocialResponderService responderService) {
        this.responderService = responderService;
    }

    /**
     * Webhook endpoint for receiving social media comments.
     * Instagram, TikTok, and Twitter can all POST here.
     */
    /**
     * Webhook verification endpoint for platforms like Meta (Instagram).
     * Meta sends a GET request to verify the webhook setup.
     */
    @GetMapping(value = "/webhook/{platform}", produces = "text/plain")
    public ResponseEntity<String> verifyWebhook(
            @PathVariable String platform,
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.challenge", required = false) String challenge,
            @RequestParam(value = "hub.verify_token", required = false) String verifyToken) {
        
        System.out.println("[Social Webhook Verification] platform: " + platform + ", mode: " + mode + ", verifyToken: " + verifyToken);
        if ("subscribe".equals(mode) && challenge != null) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.badRequest().body("Invalid verification request");
    }

    @PostMapping("/webhook/{platform}")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @PathVariable String platform,
            @RequestBody Map<String, Object> payload) {

        String commentId = "unknown";
        String postId = "unknown";
        String username = "unknown";
        String commentText = "";

        // Check if this is a standard nested Meta/Instagram webhook payload
        if ("instagram".equalsIgnoreCase(platform) && payload.containsKey("entry")) {
            try {
                List<Map<String, Object>> entries = (List<Map<String, Object>>) payload.get("entry");
                if (entries != null && !entries.isEmpty()) {
                    Map<String, Object> entry = entries.get(0);
                    List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get("changes");
                    if (changes != null && !changes.isEmpty()) {
                        Map<String, Object> change = changes.get(0);
                        Map<String, Object> value = (Map<String, Object>) change.get("value");
                        if (value != null) {
                            commentId = (String) value.get("id");
                            commentText = (String) value.get("text");
                            
                            Map<String, Object> from = (Map<String, Object>) value.get("from");
                            if (from != null) {
                                username = (String) from.get("username");
                            }
                            
                            Map<String, Object> media = (Map<String, Object>) value.get("media");
                            if (media != null) {
                                postId = (String) media.get("id");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[Instagram Webhook] Error parsing Meta Graph API nested payload: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Fallback for flat payload (manual testing / POST requests)
            commentId = String.valueOf(payload.getOrDefault("commentId", "unknown"));
            postId = String.valueOf(payload.getOrDefault("postId", "unknown"));
            username = String.valueOf(payload.getOrDefault("username", "unknown"));
            commentText = String.valueOf(payload.getOrDefault("text", ""));
        }

        SocialComment result = responderService.processIncomingComment(
                platform, commentId, postId, username, commentText);

        if (result != null) {
            return ResponseEntity.ok(Map.of(
                    "status", result.getStatus(),
                    "reply", result.getReplyText()
            ));
        }

        return ResponseEntity.ok(Map.of("status", "duplicate"));
    }

    /**
     * Get all comments for a specific platform.
     */
    @GetMapping("/comments/{platform}")
    public ResponseEntity<List<SocialComment>> getComments(@PathVariable String platform) {
        return ResponseEntity.ok(responderService.getCommentsByPlatform(platform));
    }

    /**
     * Get all comments across all platforms.
     */
    @GetMapping("/comments")
    public ResponseEntity<List<SocialComment>> getAllComments() {
        return ResponseEntity.ok(responderService.getAllComments());
    }
}
