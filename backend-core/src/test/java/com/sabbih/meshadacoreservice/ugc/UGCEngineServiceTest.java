package com.sabbih.meshadacoreservice.ugc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbih.meshadacoreservice.social.SocialPublisherService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UGCEngineServiceTest {

    private UGCVideoRepository ugcVideoRepository;
    private ObjectMapper objectMapper;
    private SocialPublisherService socialPublisherService;
    private UGCEngineService ugcEngineService;
    private File tempScript;

    @BeforeEach
    void setUp() throws IOException {
        ugcVideoRepository = mock(UGCVideoRepository.class);
        objectMapper = new ObjectMapper();
        socialPublisherService = mock(SocialPublisherService.class);

        ugcEngineService = new UGCEngineService(ugcVideoRepository, objectMapper, socialPublisherService);
        tempScript = File.createTempFile("test_orchestrator", ".py");
    }

    @AfterEach
    void tearDown() {
        if (tempScript != null && tempScript.exists()) {
            tempScript.delete();
        }
    }

    private void writeScriptContent(String pythonCode) throws IOException {
        try (FileWriter writer = new FileWriter(tempScript)) {
            writer.write(pythonCode);
        }
    }

    @Test
    void testGenerateUGCForProductSuccess() throws Exception {
        // Prepare python mock orchestrator script that outputs valid JSON list of assets
        String code = "import json\n" +
                "assets = [{\n" +
                "  'backend': 'fal',\n" +
                "  'final_video_url': 'https://example.com/video.mp4',\n" +
                "  'model_name': 'Aria',\n" +
                "  'script': 'This is a test script',\n" +
                "  'vton_image': 'https://example.com/vton.jpg'\n" +
                "}]\n" +
                "print(json.dumps(assets))\n";

        writeScriptContent(code);
        ReflectionTestUtils.setField(ugcEngineService, "scriptPath", tempScript.getAbsolutePath());

        UGCVideo mockSavedVideo = UGCVideo.builder().id(12L).itemName("T-Shirt").build();
        when(ugcVideoRepository.save(any(UGCVideo.class))).thenReturn(mockSavedVideo);

        boolean success = ugcEngineService.generateUGCForProduct(
                45L, "T-Shirt", "Details", "https://img.url", "apparel", "https://affiliate.url"
        );

        assertTrue(success);
        verify(ugcVideoRepository, times(1)).save(any(UGCVideo.class));
        verify(socialPublisherService, times(1)).publishVideoToSocial(mockSavedVideo);
    }

    @Test
    void testGenerateUGCForProductMockBackendCreditExhaustion() throws Exception {
        // Return mock backend
        String code = "import json\n" +
                "assets = [{\n" +
                "  'backend': 'mock',\n" +
                "  'final_video_url': 'https://example.com/video.mp4',\n" +
                "  'model_name': 'Aria',\n" +
                "  'script': 'This is a test script',\n" +
                "  'vton_image': 'https://example.com/vton.jpg'\n" +
                "}]\n" +
                "print(json.dumps(assets))\n";

        writeScriptContent(code);
        ReflectionTestUtils.setField(ugcEngineService, "scriptPath", tempScript.getAbsolutePath());

        boolean success = ugcEngineService.generateUGCForProduct(
                45L, "T-Shirt", "Details", "https://img.url", "apparel", "https://affiliate.url"
        );

        assertFalse(success);
        verify(ugcVideoRepository, never()).save(any(UGCVideo.class));
        verify(socialPublisherService, never()).publishVideoToSocial(any());
    }

    @Test
    void testGenerateUGCForProductScriptExitFailure() throws Exception {
        // Exit with non-zero code
        String code = "import sys\n" +
                "print('Failed to authenticate')\n" +
                "sys.exit(1)\n";

        writeScriptContent(code);
        ReflectionTestUtils.setField(ugcEngineService, "scriptPath", tempScript.getAbsolutePath());

        boolean success = ugcEngineService.generateUGCForProduct(
                45L, "T-Shirt", "Details", "https://img.url", "apparel", "https://affiliate.url"
        );

        assertFalse(success);
        verify(ugcVideoRepository, never()).save(any(UGCVideo.class));
    }

    @Test
    void testGenerateUGCForProductInvalidJsonOutput() throws Exception {
        // No json in output
        String code = "print('Completed but something went wrong')\n";

        writeScriptContent(code);
        ReflectionTestUtils.setField(ugcEngineService, "scriptPath", tempScript.getAbsolutePath());

        boolean success = ugcEngineService.generateUGCForProduct(
                45L, "T-Shirt", "Details", "https://img.url", "apparel", "https://affiliate.url"
        );

        assertFalse(success);
        verify(ugcVideoRepository, never()).save(any(UGCVideo.class));
    }

    @Test
    void testGenerateUGCForProductExceptionThrown() {
        // Set invalid script path to trigger IOException
        ReflectionTestUtils.setField(ugcEngineService, "scriptPath", "/invalid/path/to/script.py");

        boolean success = ugcEngineService.generateUGCForProduct(
                45L, "T-Shirt", "Details", "https://img.url", "apparel", "https://affiliate.url"
        );

        assertFalse(success);
        verify(ugcVideoRepository, never()).save(any(UGCVideo.class));
    }
}
