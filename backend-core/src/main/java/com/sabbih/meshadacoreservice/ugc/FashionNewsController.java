package com.sabbih.meshadacoreservice.ugc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/api/v1/fashion-news")
public class FashionNewsController {

    private final FashionNewsService newsService;
    private final FashionNewsPostRepository newsPostRepository;
    private final Executor taskExecutor;

    @Autowired
    public FashionNewsController(FashionNewsService newsService,
                                 FashionNewsPostRepository newsPostRepository,
                                 Executor taskExecutor) {
        this.newsService = newsService;
        this.newsPostRepository = newsPostRepository;
        this.taskExecutor = taskExecutor;
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateNewsPost(@RequestBody(required = false) Map<String, Boolean> payload) {
        boolean dryRun = false;
        if (payload != null && payload.containsKey("dryRun")) {
            dryRun = payload.get("dryRun");
        }
        
        final boolean finalDryRun = dryRun;
        taskExecutor.execute(() -> {
            newsService.generateAndPostNews(finalDryRun);
        });

        return ResponseEntity.ok("Fashion news infographic generation triggered in the background. (dryRun=" + dryRun + ")");
    }

    @GetMapping("/posts")
    public ResponseEntity<List<FashionNewsPost>> getLatestNewsPosts() {
        return ResponseEntity.ok(newsPostRepository.findAllByOrderByCreatedAtDesc());
    }
}
