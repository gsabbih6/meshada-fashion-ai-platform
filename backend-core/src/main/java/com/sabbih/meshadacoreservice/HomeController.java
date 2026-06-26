package com.sabbih.meshadacoreservice;

import com.sabbih.meshadacoreservice.ugc.UGCVideoRepository;
import com.sabbih.meshadacoreservice.social.SocialCommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @Value("${spring.datasource.url:not_set}")
    private String dbUrl;

    @Autowired
    private UGCVideoRepository videoRepository;

    @Autowired
    private SocialCommentRepository commentRepository;

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getHome() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "app", "Meshada Fashion AI Platform API",
                "version", "1.0.0",
                "message", "Meshada backend API entrypoint is fully functional."
        ));
    }

    @GetMapping("/debug-db")
    public ResponseEntity<Map<String, Object>> debugDb() {
        String maskedUrl = dbUrl;
        if (dbUrl != null && dbUrl.contains("@")) {
            int atIndex = dbUrl.indexOf("@");
            int colonSlashSlash = dbUrl.indexOf("://");
            if (colonSlashSlash != -1 && atIndex > colonSlashSlash) {
                maskedUrl = dbUrl.substring(0, colonSlashSlash + 3) + "******" + dbUrl.substring(atIndex);
            }
        }
        return ResponseEntity.ok(Map.of(
                "dbUrl", maskedUrl,
                "videoCount", videoRepository.count(),
                "commentCount", commentRepository.count()
        ));
    }
}
