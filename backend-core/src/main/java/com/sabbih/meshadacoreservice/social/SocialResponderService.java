package com.sabbih.meshadacoreservice.social;

import com.sabbih.meshadacoreservice.ugc.UGCVideo;
import com.sabbih.meshadacoreservice.ugc.UGCVideoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class SocialResponderService {

    private final SocialCommentRepository commentRepository;
    private final UGCVideoRepository videoRepository;
    private final CommentReplyEngine replyEngine;

    @Value("${meshada.app.url:https://www.meshada.com}")
    private String appBaseUrl;

    @Autowired
    public SocialResponderService(SocialCommentRepository commentRepository,
                                   UGCVideoRepository videoRepository,
                                   CommentReplyEngine replyEngine) {
        this.commentRepository = commentRepository;
        this.videoRepository = videoRepository;
        this.replyEngine = replyEngine;
    }

    /**
     * Process an incoming webhook comment from any social platform.
     */
    public SocialComment processIncomingComment(String platform, String commentId,
                                                  String postId, String username,
                                                  String commentText) {
        // Deduplicate
        if (commentRepository.existsByCommentIdAndPlatform(commentId, platform)) {
            log.info("Duplicate comment ignored: {} on {}", commentId, platform);
            return null;
        }

        // Find the most relevant product to link
        String productLink = findBestProductLink(commentText);

        // Generate AI reply
        String reply = replyEngine.generateReply(commentText, productLink);

        SocialComment comment = SocialComment.builder()
                .platform(platform)
                .commentId(commentId)
                .postId(postId)
                .username(username)
                .commentText(commentText)
                .replyText(reply)
                .productLink(productLink)
                .status("pending")
                .receivedAt(LocalDateTime.now())
                .build();

        commentRepository.save(comment);
        log.info("Processed comment from @{} on {}: {}", username, platform, commentText);

        // Attempt to post reply (will be platform-specific when API keys are configured)
        try {
            postReplyToPlatform(comment);
            comment.setStatus("replied");
            comment.setRepliedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.warn("Could not auto-reply to comment {}: {}. Queued for manual review.", commentId, e.getMessage());
            comment.setStatus("queued");
        }

        return commentRepository.save(comment);
    }

    /**
     * Find the best product link based on comment keywords.
     */
    private String findBestProductLink(String commentText) {
        String lower = commentText.toLowerCase();
        List<UGCVideo> videos = videoRepository.findAllByOrderByCreatedAtDesc();

        // Try to match by item name keywords
        for (UGCVideo video : videos) {
            if (video.getItemName() != null) {
                String[] words = video.getItemName().toLowerCase().split("\\s+");
                for (String word : words) {
                    if (word.length() > 3 && lower.contains(word)) {
                        return video.getAffiliateLink();
                    }
                }
            }
        }

        // Fallback: return the most recent product link
        if (!videos.isEmpty()) {
            return videos.get(0).getAffiliateLink();
        }

        return appBaseUrl;
    }

    /**
     * Post reply back to the social platform.
     * Currently logs the reply. When API keys for Instagram/TikTok/Twitter
     * are configured, this will make the actual API call.
     */
    private void postReplyToPlatform(SocialComment comment) {
        // TODO: Implement platform-specific API calls when credentials are available
        // For now, log the reply that would be posted
        log.info("[AUTO-REPLY] Platform: {} | To: @{} | Reply: {}",
                comment.getPlatform(), comment.getUsername(), comment.getReplyText());
    }

    /**
     * Retry failed replies periodically.
     */
    @Scheduled(fixedDelay = 300000) // every 5 minutes
    public void retryPendingReplies() {
        List<SocialComment> pending = commentRepository.findByStatus("pending");
        for (SocialComment comment : pending) {
            try {
                postReplyToPlatform(comment);
                comment.setStatus("replied");
                comment.setRepliedAt(LocalDateTime.now());
                commentRepository.save(comment);
            } catch (Exception e) {
                log.warn("Retry failed for comment {}: {}", comment.getCommentId(), e.getMessage());
            }
        }
    }

    public List<SocialComment> getCommentsByPlatform(String platform) {
        return commentRepository.findByPlatformOrderByReceivedAtDesc(platform);
    }

    public List<SocialComment> getAllComments() {
        return commentRepository.findAll();
    }
}
