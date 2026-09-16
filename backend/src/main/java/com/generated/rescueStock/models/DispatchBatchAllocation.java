package com.generated.rescueStock.models;

/** 批次去向：单调拨单对单批次的占用/出库/回补记录，是“原批次回补”的依据。 */
public class DispatchBatchAllocation {
  public Long id;
  public Long dispatchOrderId;
  public Long dispatchLineId;
  public Long inventoryBatchId;
  public Integer allocatedQty;
  public Integer returnedQty;
  public String stage;
  public String createdAt;
  public String updatedAt;

  public String batchNo;
  public String supplyName;
  public String unit;
  public String warehouseName;
}
