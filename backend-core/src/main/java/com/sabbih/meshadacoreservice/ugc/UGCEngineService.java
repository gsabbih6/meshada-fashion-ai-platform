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

    public boolean generateUGCForProduct(Long productId, String productName, String productDescription, String productImageUrl, String productType, String affiliateLink) {
        StringBuilder output = new StringBuilder();
        try {
            String resolvedPath = scriptPath;
            if (!new java.io.File(resolvedPath).exists()) {
                java.io.File relDirect = new java.io.File("ugc-engine/python_scripts/orchestrator.py");
                if (relDirect.exists()) {
                    resolvedPath = relDirect.getAbsolutePath();
                } else {
                    java.io.File relParent = new java.io.File("../ugc-engine/python_scripts/orchestrator.py");
                    if (relParent.exists()) {
                        resolvedPath = relParent.getAbsolutePath();
                    }
                }
            }
            
            ProcessBuilder pb = new ProcessBuilder(
                    "python3", resolvedPath,
                    "--product_id", String.valueOf(productId),
                    "--product_name", productName,
                    "--product_description", productDescription,
                    "--product_image_url", productImageUrl,
                    "--product_type", productType
            );
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
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
                    List<Map<String, Object>> generatedAssets = objectMapper.readValue(jsonArrayStr, new TypeReference<List<Map<String, Object>>>(){});
                    
                    boolean created = false;
                    for (Map<String, Object> asset : generatedAssets) {
                        String backend = (String) asset.get("backend");
                        if ("mock".equalsIgnoreCase(backend)) {
                            System.err.println("[UGC Engine] Python script returned mock backend. Treating as credit exhaustion fallback.");
                            return false;
                        }
 
                        UGCVideo video = new UGCVideo();
                        video.setUrl((String) asset.get("final_video_url"));
                        video.setAffiliateLink(affiliateLink);
                        video.setModelName((String) asset.get("model_name"));
                        video.setItemName(productName);
                        video.setScript((String) asset.get("script"));
                        video.setVtonImageUrl((String) asset.get("vton_image"));
                        video.setCreatedAt(java.time.LocalDateTime.now());
                        video.setProductId(productId);
                        
                        UGCVideo savedVideo = ugcVideoRepository.save(video);
                        socialPublisherService.publishVideoToSocial(savedVideo);
                        created = true;
                    }
                    return created;
                } else {
                    System.err.println("UGC Engine completed but no JSON output found starting with [{\".");
                    System.err.println("Full Output: " + jsonOutput);
                    return false;
                }
            } else {
                System.err.println("UGC Engine failed with exit code: " + exitCode);
                System.err.println("Output: " + output.toString());
                return false;
            }

        } catch (Exception e) {
            System.err.println("UGC Engine encountered an exception during processing. Raw output was:\n" + output.toString());
            e.printStackTrace();
            return false;
        }
    }
}
