package com.generated.rescueStock.models;

/** 库存流水：与每次库存变更同事务双写。 */
public class StockLedger {
  public Long id;
  public Long inventoryBatchId;
  public Long supplyItemId;
  public Long warehouseId;
  public Long dispatchOrderId;
  public String direction;
  public Integer changeQty;
  public Integer quantityAfter;
  public Integer heldAfter;
  public String actor;
  public String remark;
  public String createdAt;

  public String batchNo;
  public String supplyName;
}
