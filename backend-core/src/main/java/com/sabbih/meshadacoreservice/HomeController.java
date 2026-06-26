package com.sabbih.meshadacoreservice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getHome() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "app", "Meshada Fashion AI Platform API",
                "version", "1.0.0",
                "message", "Meshada backend API entrypoint is fully functional."
        ));
    }
}
