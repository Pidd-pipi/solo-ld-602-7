package com.generated.rescueStock.types;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 避难点维护请求。 */
public class ShelterPayload {
  @NotBlank public String name;
  @NotBlank public String district;
  @NotNull public Integer capacity;
  public Integer currentPopulation;
  public String contactPerson;
  public String contactPhone;
  public String riskLevel;
  public String openStatus;
}
