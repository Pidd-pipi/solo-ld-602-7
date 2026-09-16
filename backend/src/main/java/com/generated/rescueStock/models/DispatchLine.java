package com.generated.rescueStock.models;

/** 调拨行：申请口径。 */
public class DispatchLine {
  public Long id;
  public Long dispatchOrderId;
  public Long supplyItemId;
  public Integer requestedQty;
  public Integer outboundQty;

  public String supplyName;
  public String skuCode;
  public String category;
  public String unit;
}
