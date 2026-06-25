package com.sabbih.meshadacoreservice.ugc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.sabbih.meshadacoreservice.social.SocialPublisherService;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UGCEngineService {

    private final UGCVideoRepository ugcVideoRepository;
    private final ObjectMapper objectMapper;
    private final SocialPublisherService socialPublisherService;

    @org.springframework.beans.factory.annotation.Value("${meshada.ugc.scriptPath:/Users/gsabbih/IdeaProjects/meshada-fashion-platform/ugc-engine/python_scripts/orchestrator.py}")
    private String scriptPath;

    @Autowired
    public UGCEngineService(UGCVideoRepository ugcVideoRepository, ObjectMapper objectMapper, SocialPublisherService socialPublisherService) {
        this.ugcVideoRepository = ugcVideoRepository;
        this.objectMapper = objectMapper;
        this.socialPublisherService = socialPublisherService;
    }

    public void generateUGCForProduct(String productId, String productName, String productDescription, String productImageUrl, String productType, String affiliateLink) {
        try {
            
            ProcessBuilder pb = new ProcessBuilder(
                    "python3", scriptPath,
                    "--product_id", productId,
                    "--product_name", productName,
                    "--product_description", productDescription,
                    "--product_image_url", productImageUrl,
                    "--product_type", productType
            );
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                String jsonOutput = output.toString();
                int jsonStart = jsonOutput.indexOf("[{\"");
                int jsonEnd = jsonOutput.lastIndexOf("]");
                
                if (jsonStart != -1 && jsonEnd != -1) {
                    String jsonArrayStr = jsonOutput.substring(jsonStart, jsonEnd + 1);
                    List<Map<String, String>> generatedAssets = objectMapper.readValue(jsonArrayStr, new TypeReference<List<Map<String, String>>>(){});
                    
                    for (Map<String, String> asset : generatedAssets) {
                        UGCVideo video = new UGCVideo();
                        video.setUrl(asset.get("final_video_url"));
                        video.setAffiliateLink(affiliateLink);
                        video.setModelName(asset.get("model_name"));
                        video.setItemName(productName);
                        video.setScript(asset.get("script"));
                        video.setCreatedAt(java.time.LocalDateTime.now());
                        
                        UGCVideo savedVideo = ugcVideoRepository.save(video);
                        socialPublisherService.publishVideoToSocial(savedVideo);
                    }
                } else {
                    System.err.println("UGC Engine completed but no JSON output found starting with [{\".");
                    System.err.println("Full Output: " + jsonOutput);
                }
            } else {
                System.err.println("UGC Engine failed with exit code: " + exitCode);
                System.err.println("Output: " + output.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
