package com.generated.rescueStock.constants;

import java.util.List;

/**
 * 审计/业务日志模板集中地。每个核心实体至少 4 条；
 * 调拨闭环额外提供全流程模板。字段变更时必须同步改模板与调用处。
 * 占位符同 ErrorMessages，使用 {name}。
 */
public final class LogTemplates {

  public static final List<String> WAREHOUSE = List.of(
      "应急仓库建档：{name}（{district}）",
      "应急仓库更新：{name} 字段 {field} 由 {old} 变更为 {new}",
      "应急仓库状态变更：{name} 状态切换为 {status}",
      "应急仓库数据导出：操作人 {actor}");

  public static final List<String> SUPPLY_ITEM = List.of(
      "应急物资建档：{skuCode} {name}（分类 {category}）",
      "应急物资更新：{skuCode} 字段 {field} 由 {old} 变更为 {new}",
      "应急物资分类变更：{skuCode} 调整为 {category}",
      "应急物资数据导出：操作人 {actor}");

  public static final List<String> INVENTORY_BATCH = List.of(
      "库存批次入库：{batchNo} 数量 {quantity}",
      "库存批次盘点：{batchNo} 数量调整为 {quantity}",
      "库存批次质量状态变更：{batchNo} 标记为 {qualityStatus}",
      "库存批次报损：{batchNo} 报损 {quantity}");

  public static final List<String> SHELTER = List.of(
      "避难点建档：{name}（{district}）容量 {capacity}",
      "避难点更新：{name} 字段 {field} 由 {old} 变更为 {new}",
      "避难点开放状态变更：{name} 切换为 {openStatus}",
      "避难点接收记录导出：{name} 操作人 {actor}");

  // ---- 调拨闭环：每个状态动作一条，全部带 orderId，便于全链路检索 ----
  public static final String DISPATCH_CREATE =
      "调拨单 {orderId} 创建草稿（requestId={requestId}）";
  public static final String DISPATCH_SUBMIT =
      "调拨单 {orderId} 提交申请：{lineCount} 行物资已占用可用库存";
  public static final String DISPATCH_APPROVE =
      "调拨单 {orderId} 审批通过（不扣减库存，仍为占用状态）";
  public static final String DISPATCH_REJECT =
      "调拨单 {orderId} 审批驳回：{reason}，占用已全部释放";
  public static final String DISPATCH_OUTBOUND =
      "调拨单 {orderId} 出库：按 {allocCount} 个批次扣减并登记去向";
  public static final String DISPATCH_RECEIVE =
      "调拨单 {orderId} 签收确认：物资已送达避难点";
  public static final String DISPATCH_REFUSE =
      "调拨单 {orderId} 拒签：{reason}，已按原批次、原数量回补";
  public static final String DISPATCH_CANCEL =
      "调拨单 {orderId} 撤销：{reason}，已按原批次、原数量回补";

  public static final List<String> DISPATCH_ORDER = List.of(
      DISPATCH_CREATE, DISPATCH_SUBMIT, DISPATCH_APPROVE, DISPATCH_REJECT);

  private LogTemplates() {}
}
