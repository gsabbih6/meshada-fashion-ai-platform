package com.sabbih.meshadacoreservice.social;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SocialCommentRepository extends JpaRepository<SocialComment, Long> {
    List<SocialComment> findByStatus(String status);
    List<SocialComment> findByPlatformOrderByReceivedAtDesc(String platform);
    boolean existsByCommentIdAndPlatform(String commentId, String platform);
}
