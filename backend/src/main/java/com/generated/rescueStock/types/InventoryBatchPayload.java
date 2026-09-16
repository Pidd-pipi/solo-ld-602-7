package com.generated.rescueStock.types;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 库存批次入库/盘点请求（仓库员）。 */
public class InventoryBatchPayload {
  @NotNull public Long warehouseId;
  @NotNull public Long supplyItemId;
  @NotBlank public String batchNo;
  @NotNull public Integer quantity;
  public String expireAt;
  public String inboundSource;
  public String qualityStatus;
}
