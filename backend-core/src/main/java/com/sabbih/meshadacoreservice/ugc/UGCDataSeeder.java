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
                    .script("Obsessed with this Summer Dress. The fit is absolutely flawless. Tap the link to shop my exact look.")
                    .createdAt(LocalDateTime.now())
                    .published(true)
                    .build();

            UGCVideo video2 = UGCVideo.builder()
                    .url("https://www.w3schools.com/html/movie.mp4")
                    .affiliateLink("https://meshada.com/ref/456")
                    .modelName("Luma Dream Machine")
                    .itemName("Leather Jacket")
                    .script("Streetwear essential unlocked 🔥 This Leather Jacket is everything. Check the link to cop this fit.")
                    .createdAt(LocalDateTime.now().minusHours(1))
                    .published(true)
                    .build();

            UGCVideo video3 = UGCVideo.builder()
                    .url("https://www.w3schools.com/html/mov_bbb.mp4")
                    .affiliateLink("https://meshada.com/ref/789")
                    .modelName("Runway Gen-3 Alpha")
                    .itemName("Running Shoes")
                    .script("Hey! I just found the cutest Running Shoes ever. It's so good, I had to share. Link in bio! 💕")
                    .createdAt(LocalDateTime.now().minusHours(2))
                    .published(true)
                    .build();


            videoRepository.saveAll(List.of(video1, video2, video3));
            System.out.println("Seeding complete.");
        }
    }
}
