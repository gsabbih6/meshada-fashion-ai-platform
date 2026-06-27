package com.sabbih.meshadacoreservice.ugc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbih.meshadacoreservice.social.SocialPublisherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class FashionNewsService {

    private final FashionNewsPostRepository newsPostRepository;
    private final ObjectMapper objectMapper;
    private final SocialPublisherService socialPublisherService;
    private final Executor taskExecutor;

    @Value("${meshada.ugc.newsScriptPath:/Users/gsabbih/IdeaProjects/meshada-fashion-platform/ugc-engine/python_scripts/fashion_news_infographic.py}")
    private String newsScriptPath;

    @Value("${meshada.social.skimlinksId:12345X67890}")
    private String skimlinksId;

    @Autowired
    public FashionNewsService(FashionNewsPostRepository newsPostRepository,
                              ObjectMapper objectMapper,
                              SocialPublisherService socialPublisherService,
                              Executor taskExecutor) {
        this.newsPostRepository = newsPostRepository;
        this.objectMapper = objectMapper;
        this.socialPublisherService = socialPublisherService;
        this.taskExecutor = taskExecutor;
    }

    public boolean generateAndPostNews(boolean dryRun) {
        StringBuilder output = new StringBuilder();
        try {
            log.info("[Fashion News Service] Starting infographic generation (dryRun={})...", dryRun);
            
            String resolvedPath = newsScriptPath;
            if (!new java.io.File(resolvedPath).exists()) {
                java.io.File relDirect = new java.io.File("ugc-engine/python_scripts/fashion_news_infographic.py");
                if (relDirect.exists()) {
                    resolvedPath = relDirect.getAbsolutePath();
                } else {
                    java.io.File relParent = new java.io.File("../ugc-engine/python_scripts/fashion_news_infographic.py");
                    if (relParent.exists()) {
                        resolvedPath = relParent.getAbsolutePath();
                    }
                }
            }
            
            ProcessBuilder pb;
            if (dryRun) {
                pb = new ProcessBuilder(
                        "python3", resolvedPath,
                        "--dry-run",
                        "--skimlinks-id", skimlinksId
                );
            } else {
                pb = new ProcessBuilder(
                        "python3", resolvedPath,
                        "--skimlinks-id", skimlinksId
                );
            }
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                String fullOutput = output.toString();
                int jsonStart = fullOutput.indexOf("--- JSON OUTPUT START ---");
                int jsonEnd = fullOutput.indexOf("--- JSON OUTPUT END ---");
                
                if (jsonStart != -1 && jsonEnd != -1) {
                    String jsonStr = fullOutput.substring(jsonStart + "--- JSON OUTPUT START ---".length(), jsonEnd).trim();
                    Map<String, Object> result = objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
                    
                    if ("success".equalsIgnoreCase((String) result.get("status"))) {
                        java.util.List<String> urls = (java.util.List<String>) result.get("final_graphic_urls");
                        String joinedUrls = urls != null ? String.join(",", urls) : "";

                        FashionNewsPost post = FashionNewsPost.builder()
                                .imageUrls(joinedUrls)
                                .title((String) result.get("headline"))
                                .summaryText((String) result.get("slide2_text")) // Use slide 2 story text
                                .socialCaption((String) result.get("social_caption"))
                                .sourceUrl((String) result.get("original_url"))
                                .monetizedUrl((String) result.get("monetized_url"))
                                .createdAt(LocalDateTime.now())
                                .published(false)
                                .build();
                                
                        FashionNewsPost savedPost = newsPostRepository.save(post);
                        log.info("[Fashion News Service] Infographic generated and saved to DB. ID: {}", savedPost.getId());
                        
                        // Async publishing to social platforms
                        taskExecutor.execute(() -> {
                            publishNewsPostToSocial(savedPost);
                        });
                        
                        return true;
                    }
                }
                log.error("[Fashion News Service] Failed to parse JSON from Python output. Raw: {}", fullOutput);
                return false;
            } else {
                log.error("[Fashion News Service] Python script failed with exit code: {}. Output: {}", exitCode, output.toString());
                return false;
            }
        } catch (Exception e) {
            log.error("[Fashion News Service] Exception during news generation: {}", e.getMessage(), e);
            return false;
        }
    }

    private void publishNewsPostToSocial(FashionNewsPost post) {
        log.info("[Fashion News Service] Initiating social publication for News Post ID: {}", post.getId());
        
        // Wrap/Delegate to socialPublisherService
        // 1. Post to Twitter
        try {
            // SocialPublisherService has a publishToTwitter helper, but it's private.
            // We can add public bridge methods in SocialPublisherService or handle it cleanly.
            // Let's implement publishing logic directly or invoke existing ones.
            socialPublisherService.publishNewsPostBridge(post);
            post.setPublished(true);
            newsPostRepository.save(post);
            log.info("[Fashion News Service] News Post ID: {} successfully published.", post.getId());
        } catch (Exception e) {
            log.error("[Fashion News Service] Failed to publish post: {}", e.getMessage(), e);
        }
    }

    /**
     * Automatically fetch, generate, and publish fashion news infographics
     * 10 times daily at separate intervals during active waking hours.
     */
    @org.springframework.scheduling.annotation.Scheduled(cron = "${meshada.ugc.newsScheduleCron:0 0 8,10,12,13,14,15,17,19,21,22 * * *}")
    public void scheduledGenerateAndPostNews() {
        log.info("[Fashion News Service] Running scheduled news generation task...");
        generateAndPostNews(false);
    }
}
