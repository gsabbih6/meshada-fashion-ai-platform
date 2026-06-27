package com.sabbih.meshadacoreservice.ugc;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Component
@Slf4j
public class PythonEnvInitializer {

    @PostConstruct
    public void initializePythonEnv() {
        log.info("[Python Init] Starting automatic verification and installation of Python dependencies...");
        
        try {
            // Run pip3 install to make sure all required packages are present in the environment
            ProcessBuilder pb = new ProcessBuilder(
                    "pip3", "install", 
                    "Pillow", 
                    "requests", 
                    "python-dotenv", 
                    "edge-tts", 
                    "higgsfield-client",
                    "openai"
            );
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // Read output to log progress/errors
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[Python Init - pip] {}", line);
            }
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("[Python Init] Successfully verified/installed all Python dependencies.");
            } else {
                log.warn("[Python Init] pip3 install exited with non-zero code: {}", exitCode);
            }
        } catch (Exception e) {
            log.error("[Python Init] Failed to run pip3 installation: {}", e.getMessage(), e);
        }
    }
}
