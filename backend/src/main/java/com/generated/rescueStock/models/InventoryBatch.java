package com.generated.rescueStock.models;

/**
 * 库存批次。available 不落地，始终由 quantity - heldQuantity 实时计算，
 * 避免“占用量”和“可用量”两份事实不一致。
 */
public class InventoryBatch {
  public Long id;
  public Long warehouseId;
  public Long supplyItemId;
  public String batchNo;
  public Integer quantity;
  public Integer heldQuantity;
  public String expireAt;
  public String inboundSource;
  public String qualityStatus;

  // ---- 关联展示字段（联表查询填充） ----
  public String supplyName;
  public String skuCode;
  public String category;
  public String unit;
  public String warehouseName;
}
