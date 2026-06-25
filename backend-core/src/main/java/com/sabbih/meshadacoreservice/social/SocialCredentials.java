package com.sabbih.meshadacoreservice.social;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "social_credentials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialCredentials {

    @Id
    private String platform; // e.g., "instagram", "pinterest", "twitter", "tiktok"

    private String accessToken;
    private String businessAccountId;
    private LocalDateTime updatedAt;
}
