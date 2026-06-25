package com.sabbih.meshadacoreservice.social;

import com.sabbih.meshadacoreservice.ugc.UGCVideo;
import com.sabbih.meshadacoreservice.ugc.UGCVideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SocialPinterestTests {

    private SocialCommentRepository commentRepository;
    private UGCVideoRepository videoRepository;
    private CommentReplyEngine replyEngine;
    private SocialPlatformClient platformClient;
    private SocialResponderService responderService;

    @BeforeEach
    void setUp() {
        commentRepository = mock(SocialCommentRepository.class);
        videoRepository = mock(UGCVideoRepository.class);
        replyEngine = mock(CommentReplyEngine.class);
        platformClient = mock(SocialPlatformClient.class);
        responderService = new SocialResponderService(commentRepository, videoRepository, replyEngine, platformClient);
    }

    @Test
    void testReplyToPinterestCommentReturnsFalse() {
        SocialPlatformClient client = new SocialPlatformClient(WebClient.builder());
        boolean result = client.replyToPinterestComment("123", "Nice!");
        assertFalse(result);
    }

    @Test
    void testProcessIncomingPinterestCommentQueuesCommentDueToUnsupportedPlatform() {
        // Arrange
        String commentId = "pin-comment-123";
        String postId = "pin-123";
        String username = "testuser";
        String text = "Wow! Need this";
        
        when(commentRepository.existsByCommentIdAndPlatform(commentId, "pinterest")).thenReturn(false);
        when(replyEngine.generateReply(anyString(), anyString())).thenReturn("Omg yes!");
        when(commentRepository.save(any(SocialComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SocialComment comment = responderService.processIncomingComment("pinterest", commentId, postId, username, text);

        // Assert
        assertNotNull(comment);
        assertEquals("queued", comment.getStatus());
    }

    @Test
    void testSocialPublisherPinterestPublishingGracefullyHandlesEmptyCredentials() {
        // Create publisher with empty WebClient
        SocialPublisherService publisher = new SocialPublisherService(WebClient.builder());
        
        UGCVideo video = UGCVideo.builder()
                .id(1L)
                .url("https://example.com/video.mp4")
                .vtonImageUrl("https://example.com/image.png")
                .itemName("Cool Shirt")
                .affiliateLink("https://example.com/shop")
                .modelName("Aria")
                .build();

        // Should not throw any exception when config is blank (should skip gracefully)
        assertDoesNotThrow(() -> publisher.publishVideoToSocial(video));
    }
}
