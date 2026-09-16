package com.generated.rescueStock.constants;

/** 库存流水方向与批次占用阶段，前后端常量同名同义。 */
public final class LedgerDirection {
  public static final String HOLD = "HOLD";           // 申请占用：held +
  public static final String RELEASE = "RELEASE";     // 驳回：held -
  public static final String OUTBOUND = "OUTBOUND";   // 出库：quantity - 且 held -
  public static final String RETURN = "RETURN";       // 拒签/撤销回补

  public static final String STAGE_HELD = "HELD";
  public static final String STAGE_OUT = "OUT";
  public static final String STAGE_RETURNED = "RETURNED";

  private LedgerDirection() {}
}
