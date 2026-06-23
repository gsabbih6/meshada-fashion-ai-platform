package com.sabbih.meshadacoreservice.ugc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UGCVideoRepository extends JpaRepository<UGCVideo, Long> {
    List<UGCVideo> findAllByOrderByCreatedAtDesc();
}
