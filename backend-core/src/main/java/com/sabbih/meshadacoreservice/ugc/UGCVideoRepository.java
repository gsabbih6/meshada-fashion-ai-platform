package com.sabbih.meshadacoreservice.ugc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UGCVideoRepository extends JpaRepository<UGCVideo, Long> {
    List<UGCVideo> findAllByOrderByCreatedAtDesc();

    @Query("SELECT v FROM UGCVideo v WHERE v.url NOT LIKE '%.mp4'")
    List<UGCVideo> findPlaceholderVideos();

    @Query("SELECT v FROM UGCVideo v WHERE v.url LIKE '%.mp4' AND v.url NOT LIKE '%w3schools%'")
    List<UGCVideo> findGeneratedVideos();

    @Query("SELECT v FROM UGCVideo v WHERE v.url LIKE '%.mp4' AND v.url NOT LIKE '%w3schools%' AND (v.published IS NULL OR v.published = false)")
    List<UGCVideo> findUnpublishedVideos();
}
