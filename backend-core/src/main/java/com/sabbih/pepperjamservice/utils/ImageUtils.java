package com.sabbih.pepperjamservice.utils;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
@Slf4j
public class ImageUtils {

  public static boolean isRealImage(String imageUrl) {
//    log.info("Checking image  product: {}", imageUrl);
    try {
      URL url = new URL(imageUrl);
      BufferedImage image = ImageIO.read(url);
      return image != null && image.getWidth() > 0 && image.getHeight() > 0;
    } catch (IOException e) {
//      log.info("Checking image  product: {}", imageUrl);

      return false;
    }
  }

  public static BufferedImage getImage(String imageUrl) {
    try {
      URL url = new URL(imageUrl);
      BufferedImage image = ImageIO.read(url);
      return image;
    } catch (IOException e) {
      return null;
    }
  }
}
