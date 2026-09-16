package com.generated.rescueStock.constants;

import java.util.HashMap;
import java.util.Map;

/** 错误消息模板，占位符用 {name} 形式，由 Formatters.fill 渲染。 */
public final class ErrorMessages {
  private static final Map<String, String> TEMPLATES = new HashMap<>();
  static {
    TEMPLATES.put(ErrorCodes.AUTH_REQUIRED, "请先登录后再执行该操作");
    TEMPLATES.put(ErrorCodes.AUTH_INVALID, "登录凭据无效或已过期");
    TEMPLATES.put(ErrorCodes.RBAC_DENIED, "当前角色 {role} 无权执行 {action}");
    TEMPLATES.put(ErrorCodes.VALIDATION_FAILED, "表单校验未通过：{reason}");
    TEMPLATES.put(ErrorCodes.RATE_LIMITED, "操作过于频繁，请稍后再试");

    TEMPLATES.put(ErrorCodes.ORDER_NOT_FOUND, "调拨单 {orderId} 不存在");
    TEMPLATES.put(ErrorCodes.ORDER_DUPLICATE_REQUEST,
        "同一申请 requestId={requestId} 已提交，已返回原调拨单，不会重复占用库存");
    TEMPLATES.put(ErrorCodes.IDEMPOTENT_CONFLICT,
        "requestId={requestId} 已被内容不同的申请占用（原单 #{orderId}）：{reason}。请为新申请生成新的 requestId，本次请求未占用任何库存");
    TEMPLATES.put(ErrorCodes.ILLEGAL_STATUS_TRANSITION,
        "调拨单 {orderId} 当前状态 {from} 不允许执行 {action}（目标状态 {to}）");
    TEMPLATES.put(ErrorCodes.BATCH_NOT_FOUND, "库存批次 {batchId} 不存在");
    TEMPLATES.put(ErrorCodes.INSUFFICIENT_STOCK,
        "批次 {batchNo}（物资 {itemName}）可用量不足：需要 {need}，可用 {available}");
    TEMPLATES.put(ErrorCodes.BATCH_CONTENDED,
        "批次 {batchNo} 已被其他避难点抢先占用，请刷新后重新选择批次");
    TEMPLATES.put(ErrorCodes.WAREHOUSE_MISMATCH,
        "批次 {batchNo} 不属于源仓库 {warehouseId}");
    TEMPLATES.put(ErrorCodes.LINE_EMPTY, "调拨单至少需要一条物资明细");
    TEMPLATES.put(ErrorCodes.ALLOCATION_MISSING,
        "调拨单 {orderId} 缺少批次去向记录，无法出库/回补");
    TEMPLATES.put(ErrorCodes.LEDGER_IMBALANCE,
        "批次 {batchId} 库存与占用不平衡，已回滚本次操作");
    TEMPLATES.put(ErrorCodes.CONCURRENT_CONFLICT, "并发冲突，请重试本次操作");
    TEMPLATES.put(ErrorCodes.INTERNAL_ERROR, "服务内部错误，请稍后再试");
  }

  public static String of(String code) {
    return TEMPLATES.getOrDefault(code, code);
  }

  public static String render(String code, Map<String, Object> args) {
    String text = of(code);
    if (args != null) {
      for (Map.Entry<String, Object> e : args.entrySet()) {
        text = text.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
      }
    }
    return text;
  }

  private ErrorMessages() {}
}
