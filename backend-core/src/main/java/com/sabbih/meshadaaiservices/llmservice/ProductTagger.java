package com.sabbih.meshadaaiservices.llmservice;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import java.util.List;

public interface ProductTagger {
  @SystemMessage("Use product categories from this list only: {{category}}, " + "\n" + "\"\"")
  //    @SystemMessage("You are a professional data entry specialist so avoid redundancy")
  @UserMessage(
      "Extract fashion product category list from the description: {{text}}. At least 2 categories is a hierarchical order")
  List<String> tags(@V("text") String text, @V("category") String category);

  @UserMessage("Rewrite the product description: " + "{{description}}" + "for a fashion website")
  String description(@V("description") String description);
}
