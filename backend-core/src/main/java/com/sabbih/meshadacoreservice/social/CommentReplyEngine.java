package com.sabbih.meshadacoreservice.social;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Random;

@Service
public class CommentReplyEngine {

    private static final List<String> REPLY_TEMPLATES = List.of(
        "Omg yes! 🔥 You can get this exact look here: %s",
        "So glad you love it! ✨ Shop it directly: %s",
        "This one's a showstopper right?! 💕 Grab it here: %s",
        "You have amazing taste! 🛍️ Get yours: %s",
        "It's giving everything! ✨ Link to shop: %s",
        "Obsessed with this piece too! 🤩 Shop now: %s",
        "Right?! 💅 Here's where to get it: %s",
        "Thanks babe! 💖 You can shop this look: %s"
    );

    private final Random random = new Random();

    public String generateReply(String commentText, String productLink) {
        // Select a random template and insert the product link
        String template = REPLY_TEMPLATES.get(random.nextInt(REPLY_TEMPLATES.size()));
        return String.format(template, productLink);
    }
}
