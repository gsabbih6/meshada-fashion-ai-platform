package com.sabbih.meshadacoreservice.ugc;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "fashion_news_posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FashionNewsPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String imageUrls;
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String summaryText;
    
    private String sourceUrl;
    private String monetizedUrl;
    private String socialCaption;
    private LocalDateTime createdAt;

    @Builder.Default
    private Boolean published = false;

    private String instagramPostId;
    private String facebookPostId;
    private String twitterPostId;
    private String pinterestPinId;
}
