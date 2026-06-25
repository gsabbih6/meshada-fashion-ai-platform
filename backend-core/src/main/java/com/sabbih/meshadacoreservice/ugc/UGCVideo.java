package com.sabbih.meshadacoreservice.ugc;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ugc_videos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UGCVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String url;
    private String affiliateLink;
    private String modelName;
    private String itemName;
    
    @jakarta.persistence.Column(columnDefinition = "TEXT")
    private String script;
    
    private LocalDateTime createdAt;
}

