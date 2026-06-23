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
    @PostMapping("/webhook/{platform}")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @PathVariable String platform,
            @RequestBody Map<String, String> payload) {

        String commentId = payload.getOrDefault("commentId", "unknown");
        String postId = payload.getOrDefault("postId", "unknown");
        String username = payload.getOrDefault("username", "unknown");
        String commentText = payload.getOrDefault("text", "");

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
