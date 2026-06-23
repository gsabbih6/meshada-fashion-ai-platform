package com.sabbih.meshadaaiservices.utils;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.ml.recommendation.ALSModel;

@Slf4j
public class ModelLoader {

  public static ALSModel loadLatestModel(String modelDirPath) {
    try {
      Optional<Path> latestModelPath =
          Files.walk(Paths.get(modelDirPath), 1)
              .filter(Files::isDirectory)
              .max(Comparator.comparingLong(path -> path.toFile().lastModified()));

      if (latestModelPath.isPresent()) {
        log.info("Loading model from {}", modelDirPath);
        return ALSModel.load(modelDirPath);
      } else {
        log.warn("No saved models found in {}", modelDirPath);
        return null;
      }
    } catch (IOException e) {
      log.error("Error finding the latest model in {}: {}", modelDirPath, e.getMessage());
      return null;
    }
  }
}
