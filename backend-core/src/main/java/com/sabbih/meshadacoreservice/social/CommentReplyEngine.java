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

    private static final List<String> INSTAGRAM_PUBLIC_REPLIES = List.of(
        "Just sent the link to your DMs! Check your messages 💌",
        "Check your inbox! DMed you the link 🛍️",
        "Sent you a DM with the link! Let me know if you get it 💕",
        "Just slid into your DMs with the link! 😘",
        "DMed you the link! Happy shopping! ✨"
    );

    private final Random random = new Random();

    public String generateReply(String commentText, String productLink) {
        // Select a random template and insert the product link
        String template = REPLY_TEMPLATES.get(random.nextInt(REPLY_TEMPLATES.size()));
        return String.format(template, productLink);
    }

    public String generateInstagramPublicReply() {
        return INSTAGRAM_PUBLIC_REPLIES.get(random.nextInt(INSTAGRAM_PUBLIC_REPLIES.size()));
    }
}

