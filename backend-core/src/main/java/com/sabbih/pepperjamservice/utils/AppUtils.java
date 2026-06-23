package com.sabbih.pepperjamservice.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppUtils {
  public static String[] dateNowPlus(long monthsToAdd) {
    LocalDate currentDate = LocalDate.now();
    LocalDate dateInSixMonths = currentDate.plusMonths(monthsToAdd);

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    String formattedDateNow = currentDate.format(formatter);
    String formattedDatePlus = dateInSixMonths.format(formatter);
    return new String[] {formattedDateNow, formattedDatePlus};
  }

  public static String[] dateNowMinus(long monthsToMinus) {
    LocalDate currentDate = LocalDate.now();
    LocalDate dateInSixMonths = currentDate.minusMonths(monthsToMinus);

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    String formattedDateNow = currentDate.format(formatter);
    String formattedDateMinus = dateInSixMonths.format(formatter);
    return new String[] {formattedDateMinus, formattedDateNow};
  }

  public static String generateSlug(String productName) {
    String slug = productName.toLowerCase(); // Convert to lowercase
    slug = slug.replace(" ", "-"); // Replace spaces with hyphens
    slug = slug.replace("[^a-z0-9-]", ""); // Remove non-alphanumeric characters
    return slug;
  }

  public static String extractFirstAlphanumeric(String input) {
    // Regular expression to match alphanumeric strings including underscores
    Pattern pattern = Pattern.compile("[a-zA-Z0-9_]+");
    Matcher matcher = pattern.matcher(input);

    // Find the first match
    if (matcher.find()) {
      return matcher.group(
          0); // Returns the first sequence of alphanumeric characters including underscores
    }

    // Return the original input if no match is found or return an empty string or null depending on
    // your error handling strategy
    return input; // or return "";
  }
}
