package com.sabbih.meshadacoreservice.ugc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FashionNewsPostRepository extends JpaRepository<FashionNewsPost, Long> {
    List<FashionNewsPost> findAllByOrderByCreatedAtDesc();
}
