/** 审计/业务日志模板（前端副本），与后端 LogTemplates 对应；操作成功提示与审计抽屉共用。 */
export const LOG_TEMPLATES = {
  DispatchOrder: {
    CREATE: "调拨单 {orderId} 创建（requestId={requestId}）",
    SUBMIT: "调拨单 {orderId} 提交：{lineCount} 行物资已占用可用库存",
    APPROVE: "调拨单 {orderId} 审批通过（不扣减库存）",
    REJECT: "调拨单 {orderId} 审批驳回：{reason}，占用已全部释放",
    OUTBOUND: "调拨单 {orderId} 出库：按批次扣减并登记去向",
    RECEIVE: "调拨单 {orderId} 签收确认",
    REFUSE: "调拨单 {orderId} 拒签：{reason}，已按原批次回补",
    CANCEL: "调拨单 {orderId} 撤销：{reason}，已按原批次回补"
  },
  InventoryBatch: [
    "库存批次入库：{batchNo} 数量 {quantity}",
    "库存批次盘点：{batchNo} 调整为 {quantity}",
    "库存批次质量变更：{batchNo} 标记为 {qualityStatus}",
    "库存批次报损：{batchNo} 报损 {quantity}"
  ],
  Shelter: [
    "避难点建档：{name}",
    "避难点更新：{name}",
    "避难点状态变更：{name} 切换为 {openStatus}",
    "避难点接收记录导出：{name}"
  ]
} as const;

export const renderLog = (tpl: string, vars: Record<string, string | number> = {}): string =>
  tpl.replace(/\{(\w+)\}/g, (_, k) => (k in vars ? String(vars[k]) : `{${k}}`));
