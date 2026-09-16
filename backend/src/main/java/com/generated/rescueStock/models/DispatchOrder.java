package com.generated.rescueStock.models;

/** 调拨单主表，状态机见 {@link com.generated.rescueStock.constants.DispatchStatus}。 */
public class DispatchOrder {
  public Long id;
  public String requestId;
  public Long eventId;
  public Long sourceWarehouseId;
  public Long shelterId;
  public String priority;
  public String status;
  public String remark;
  public Long requestedBy;
  public Long approvedBy;
  public Long refusedBy;
  public Long canceledBy;
  public String rejectReason;
  public String refuseReason;
  public String cancelReason;
  public String submittedAt;
  public String approvedAt;
  public String dispatchedAt;
  public String receivedAt;
  public String refusedAt;
  public String canceledAt;

  // ---- 展示字段 ----
  public String warehouseName;
  public String shelterName;
  public String eventTitle;
  public String requesterName;
}
