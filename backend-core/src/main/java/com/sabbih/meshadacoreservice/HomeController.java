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
}
