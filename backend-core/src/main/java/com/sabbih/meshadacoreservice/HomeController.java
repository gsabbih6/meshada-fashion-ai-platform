package com.sabbih.meshadacoreservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

@RestController
public class HomeController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getHome() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "app", "Meshada Fashion AI Platform API",
                "version", "1.0.0",
                "message", "Meshada backend API entrypoint is fully functional."
        ));
    }

    @GetMapping("/db")
    public ResponseEntity<Map<String, Object>> getDbInfo() {
        String url = "unknown";
        try (Connection conn = dataSource.getConnection()) {
            url = conn.getMetaData().getURL();
        } catch (Exception e) {
            url = "error: " + e.getMessage();
        }
        return ResponseEntity.ok(Map.of("datasource_url", url));
    }

    @GetMapping("/update-links")
    public ResponseEntity<Map<String, Object>> updateLinks() {
        int ugcRows = 0;
        int commentRows = 0;
        try (Connection conn = dataSource.getConnection()) {
            try (java.sql.PreparedStatement stmt1 = conn.prepareStatement(
                    "UPDATE ugc_videos SET affiliate_link = REPLACE(affiliate_link, 'unisex-tank-top1.html', 'unisex-jersey-tank-top') WHERE affiliate_link LIKE '%unisex-tank-top1.html%'")) {
                ugcRows = stmt1.executeUpdate();
            }
            try (java.sql.PreparedStatement stmt2 = conn.prepareStatement(
                    "UPDATE social_comments SET product_link = REPLACE(product_link, 'unisex-tank-top1.html', 'unisex-jersey-tank-top') WHERE product_link LIKE '%unisex-tank-top1.html%'")) {
                commentRows = stmt2.executeUpdate();
            }
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "ugc_videos_updated", ugcRows,
                    "social_comments_updated", commentRows
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }
}
