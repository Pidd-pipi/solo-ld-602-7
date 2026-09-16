package com.generated.rescueStock.types;

/** 审批/出库/签收/拒签/撤销动作的通用请求体。操作人以 JWT 为准，reason 用于驳回/拒签/撤销。 */
public class DispatchActionPayload {
  public String reason;
}
