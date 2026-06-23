package com.sabbih.meshadacoreservice.social;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "social_comments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SocialComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String platform; // instagram, tiktok, twitter
    private String commentId;
    private String postId;
    private String username;
    
    @Column(length = 2000)
    private String commentText;
    
    @Column(length = 2000)
    private String replyText;
    
    private String productLink;
    private String status; // pending, replied, failed
    private LocalDateTime receivedAt;
    private LocalDateTime repliedAt;
}
