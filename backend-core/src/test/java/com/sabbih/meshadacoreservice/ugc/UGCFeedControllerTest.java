package com.sabbih.meshadacoreservice.ugc;

import com.sabbih.meshadacoreservice.social.SocialPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class UGCFeedControllerTest {

    private MockMvc mockMvc;
    private UGCVideoRepository videoRepository;
    private UGCEngineService ugcEngineService;
    private UGCAutoSchedulerService schedulerService;
    private SocialPublisherService socialPublisherService;
    private Executor taskExecutor;

    @BeforeEach
    void setUp() {
        videoRepository = mock(UGCVideoRepository.class);
        ugcEngineService = mock(UGCEngineService.class);
        schedulerService = mock(UGCAutoSchedulerService.class);
        socialPublisherService = mock(SocialPublisherService.class);
        taskExecutor = mock(Executor.class);

        // Mock executor to run tasks synchronously in tests
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(taskExecutor).execute(any(Runnable.class));

        UGCFeedController controller = new UGCFeedController(
                videoRepository, ugcEngineService, schedulerService, socialPublisherService, taskExecutor
        );
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "appUrl", "https://www.meshada.com");

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testGetFeed() throws Exception {
        UGCVideo video = UGCVideo.builder().id(1L).itemName("Video 1").build();
        Page<UGCVideo> page = new PageImpl<>(List.of(video));
        when(videoRepository.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/ugc/feed?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].itemName").value("Video 1"));

        verify(videoRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void testGenerateVideoSuccess() throws Exception {
        String payload = "{" +
                "\"productId\":\"123\"," +
                "\"productName\":\"Shirt\"," +
                "\"productDescription\":\"Cool shirt\"," +
                "\"productImageUrl\":\"http://image.jpg\"," +
                "\"productType\":\"apparel\"," +
                "\"affiliateLink\":\"http://buy.me\"" +
                "}";

        mockMvc.perform(post("/api/v1/ugc/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("Video generation started in the background."));

        verify(taskExecutor, times(1)).execute(any(Runnable.class));
        verify(ugcEngineService, times(1)).generateUGCForProduct(
                eq(123L), eq("Shirt"), eq("Cool shirt"), eq("http://image.jpg"), eq("apparel"), eq("http://buy.me")
        );
    }

    @Test
    void testGenerateVideoInvalidProductId() throws Exception {
        String payload = "{" +
                "\"productId\":\"not_a_number\"," +
                "\"productName\":\"Shirt\"" +
                "}";

        mockMvc.perform(post("/api/v1/ugc/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());

        verify(taskExecutor, times(1)).execute(any(Runnable.class));
        verify(ugcEngineService, times(1)).generateUGCForProduct(
                eq(0L), eq("Shirt"), any(), any(), any(), any()
        );
    }

    @Test
    void testTriggerScheduler() throws Exception {
        mockMvc.perform(post("/api/v1/ugc/scheduler/trigger"))
                .andExpect(status().isOk())
                .andExpect(content().string("UGC posting scheduler triggered manually in the background."));

        verify(taskExecutor, times(1)).execute(any(Runnable.class));
        verify(schedulerService, times(1)).runDailyUGCPost();
    }

    @Test
    void testPostVideoManuallySuccess() throws Exception {
        UGCVideo video = UGCVideo.builder()
                .id(1L)
                .itemName("Leather Jacket")
                .url("https://example.com/video.mp4")
                .published(false)
                .build();

        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

        mockMvc.perform(post("/api/v1/ugc/post/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("UGC Video publication started in the background for ID: 1"));

        verify(videoRepository, times(1)).findById(1L);
        verify(taskExecutor, times(1)).execute(any(Runnable.class));
        verify(socialPublisherService, times(1)).publishVideoToSocial(video);
    }

    @Test
    void testPostVideoManuallyNotFound() throws Exception {
        when(videoRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/ugc/post/99"))
                .andExpect(status().isNotFound());

        verify(videoRepository, times(1)).findById(99L);
        verify(taskExecutor, never()).execute(any(Runnable.class));
        verify(socialPublisherService, never()).publishVideoToSocial(any());
    }

    @Test
    void testGetDebugConfig() throws Exception {
        mockMvc.perform(get("/api/v1/ugc/debug-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appUrl_resolved").exists());
    }
}
