package com.generated.rescueStock.types;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 调拨申请请求。
 * requestId 由申请端生成（UUID/业务单号），用于“同一申请重复提交不重复占库”的幂等控制。
 */
public class DispatchApplyPayload {
  public String requestId;
  public Long eventId;
  @NotNull public Long sourceWarehouseId;
  @NotNull public Long shelterId;
  public String priority;
  public String remark;
  @NotEmpty @Valid public List<Line> lines;

  public static class Line {
    @NotNull public Long supplyItemId;
    @NotNull @Min(1) public Integer requestedQty;
  }
}
