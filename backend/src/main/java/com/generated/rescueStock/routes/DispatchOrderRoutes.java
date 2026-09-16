package com.generated.rescueStock.routes;

/** API 路径常量，按实体分文件；controller 的 RequestMapping 必须引用此处。 */
public final class DispatchOrderRoutes {
  public static final String BASE = "/api/dispatch-orders";
  public static final String ACTION_APPROVE = "approve";
  public static final String ACTION_REJECT = "reject";
  public static final String ACTION_OUTBOUND = "outbound";
  public static final String ACTION_RECEIVE = "receive";
  public static final String ACTION_REFUSE = "refuse";
  public static final String ACTION_CANCEL = "cancel";
  public static final String SUB_ALLOCATIONS = "allocations";
  public static final String SUB_LINES = "lines";
  public static final String SUB_TIMELINE = "timeline";

  private DispatchOrderRoutes() {}
}
