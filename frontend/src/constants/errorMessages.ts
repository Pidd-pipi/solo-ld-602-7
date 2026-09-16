import { ERROR_CODES } from "./errorCodes";

/**
 * 错误消息模板与后端 ErrorMessages 对齐；{name} 占位由后端渲染后返回，
 * 前端这里保留兜底文案，便于离线演示与按钮/表单即时提示。
 */
export const ERROR_MESSAGES: Record<string, string> = {
  [ERROR_CODES.AUTH_REQUIRED]: "请先登录后再执行该操作",
  [ERROR_CODES.AUTH_INVALID]: "用户名或密码错误",
  [ERROR_CODES.RBAC_DENIED]: "当前角色没有执行该动作的权限",
  [ERROR_CODES.VALIDATION_FAILED]: "表单字段缺失或格式错误",
  [ERROR_CODES.RATE_LIMITED]: "请求过于频繁，请稍后再试",
  [ERROR_CODES.ORDER_NOT_FOUND]: "调拨单不存在",
  [ERROR_CODES.ORDER_DUPLICATE_REQUEST]: "该申请已提交过，已返回原调拨单，未重复占用库存",
  [ERROR_CODES.IDEMPOTENT_CONFLICT]: "requestId 已被内容不同的申请占用，请为新申请生成新的 requestId（本次未占用库存）",
  [ERROR_CODES.ILLEGAL_STATUS_TRANSITION]: "当前状态不允许执行该操作",
  [ERROR_CODES.INSUFFICIENT_STOCK]: "可用库存不足，申请未占用任何库存",
  [ERROR_CODES.BATCH_CONTENDED]: "该批次已被其他避难点抢先占用，请刷新后重试",
  [ERROR_CODES.CONCURRENT_CONFLICT]: "并发冲突，请刷新后重试",
  [ERROR_CODES.LEDGER_IMBALANCE]: "库存账实不平衡，本次操作已全部回滚",
  [ERROR_CODES.ALLOCATION_MISSING]: "缺少批次去向记录，无法出库或回补",
  [ERROR_CODES.INTERNAL_ERROR]: "服务内部错误，请稍后再试"
};

export const errorText = (code?: string | null, fallback?: string): string =>
  (code && ERROR_MESSAGES[code]) || fallback || "操作失败，请稍后再试";
