package com.sabbih.meshadacoreservice.ugc;

import com.sabbih.meshadacoreservice.social.SocialPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.concurrent.Executor;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

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

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
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
}
