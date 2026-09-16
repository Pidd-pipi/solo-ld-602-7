package com.generated.rescueStock.constants;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 调拨单状态机。
 * SUBMITTED 已占库；APPROVED 不扣减库存；DISPATCHED 已按批次出库；
 * REJECTED/REFUSED/CANCELLED 均会把原批次占用或出库量精确回补。
 */
public enum DispatchStatus {
  DRAFT,
  SUBMITTED,
  APPROVED,
  DISPATCHED,
  RECEIVED,
  REJECTED,   // 审批驳回：释放占用
  REFUSED,    // 到货拒签：原批次回补
  CANCELLED;  // 主动撤销：原批次回补

  /** 合法状态迁移；非法迁移一律拒绝，闭环不可跳跃或回退。 */
  public boolean canTransitTo(DispatchStatus target) {
    return TRANSITIONS.getOrDefault(this, EnumSet.noneOf(DispatchStatus.class)).contains(target);
  }

  public String label() {
    return LABELS.getOrDefault(this, name());
  }

  private static final Map<DispatchStatus, Set<DispatchStatus>> TRANSITIONS = Map.of(
      DRAFT, EnumSet.of(SUBMITTED, CANCELLED),
      SUBMITTED, EnumSet.of(APPROVED, REJECTED, CANCELLED),
      APPROVED, EnumSet.of(DISPATCHED, CANCELLED),
      DISPATCHED, EnumSet.of(RECEIVED, REFUSED)
  );

  private static final Map<DispatchStatus, String> LABELS = Map.of(
      DRAFT, "草稿",
      SUBMITTED, "待审批",
      APPROVED, "已批准",
      DISPATCHED, "已出库",
      RECEIVED, "已签收",
      REJECTED, "已驳回",
      REFUSED, "已拒签",
      CANCELLED, "已撤销"
  );
}
