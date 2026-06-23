package com.sabbih.meshadaaiservices.model;

import com.sabbih.meshadaaiservices.utils.ProductInteractionType;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductInteraction {
  private Long id;
  private Long userId;
  private ProductInteractionType interactionType;
  private Long productId;
  private Instant createdAt;
}
