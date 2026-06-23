package com.sabbih.meshadacoreservice.ugc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class UGCDataSeeder implements CommandLineRunner {

    private final UGCVideoRepository videoRepository;

    @Autowired
    public UGCDataSeeder(UGCVideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (videoRepository.count() == 0) {
            System.out.println("Seeding UGC Video Database...");
            
            UGCVideo video1 = UGCVideo.builder()
                    .url("https://www.w3schools.com/html/mov_bbb.mp4")
                    .affiliateLink("https://meshada.com/ref/123")
                    .modelName("Veo 3")
                    .itemName("Summer Dress")
                    .createdAt(LocalDateTime.now())
                    .build();

            UGCVideo video2 = UGCVideo.builder()
                    .url("https://www.w3schools.com/html/movie.mp4")
                    .affiliateLink("https://meshada.com/ref/456")
                    .modelName("Luma Dream Machine")
                    .itemName("Leather Jacket")
                    .createdAt(LocalDateTime.now().minusHours(1))
                    .build();

            UGCVideo video3 = UGCVideo.builder()
                    .url("https://www.w3schools.com/html/mov_bbb.mp4")
                    .affiliateLink("https://meshada.com/ref/789")
                    .modelName("Runway Gen-3 Alpha")
                    .itemName("Running Shoes")
                    .createdAt(LocalDateTime.now().minusHours(2))
                    .build();

            videoRepository.saveAll(List.of(video1, video2, video3));
            System.out.println("Seeding complete.");
        }
    }
}
