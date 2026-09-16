package com.generated.rescueStock.types;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 灾害事件登记请求。 */
public class DisasterEventPayload {
  @NotBlank public String title;
  @NotBlank public String district;
  public String level;
}
