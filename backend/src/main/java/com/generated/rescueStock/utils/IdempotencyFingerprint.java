package com.generated.rescueStock.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 幂等内容指纹：requestId 标识“同一次申请”，是否真的是同一次申请要看业务内容。
 *
 * 归一化规则（与请求中明细的书写顺序、是否拆成多行无关）：
 *  - 同一物资的多行按 supplyItemId 合并求和；
 *  - 物资按 id 升序排列；
 *  - 与源仓库 / 避难点 / 事件一起拼成稳定字符串。
 *
 * 这样“相同 requestId 但换了避难点或物资”能被明确识别为冲突，
 * 而“明细顺序相反、内容相同”的重试仍判定为同一申请。
 */
public final class IdempotencyFingerprint {

  /** 一条归一化后的物资需求。 */
  public record ItemQty(long supplyItemId, int qty) {}

  public static List<ItemQty> merge(List<ItemQty> lines) {
    TreeMap<Long, Integer> byItem = new TreeMap<>();
    if (lines != null) {
      for (ItemQty line : lines) {
        if (line == null) {
          continue;
        }
        byItem.merge(line.supplyItemId, line.qty, Integer::sum);
      }
    }
    List<ItemQty> out = new ArrayList<>();
    byItem.forEach((itemId, qty) -> out.add(new ItemQty(itemId, qty)));
    return out;
  }

  public static String build(long warehouseId, long shelterId, Long eventId, List<ItemQty> lines) {
    StringBuilder sb = new StringBuilder("w=").append(warehouseId)
        .append("|s=").append(shelterId)
        .append("|e=").append(eventId == null ? 0 : eventId)
        .append("|l=");
    List<ItemQty> canonical = merge(lines);
    for (int i = 0; i < canonical.size(); i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(canonical.get(i).supplyItemId).append(':').append(canonical.get(i).qty);
    }
    return sb.toString();
  }

  /** 从已落库的调拨单 + 明细行重建指纹（明细列名为下划线风格）。 */
  public static String fromStored(long warehouseId, long shelterId, Long eventId,
                                  List<Map<String, Object>> storedLines) {
    List<ItemQty> lines = new ArrayList<>();
    for (Map<String, Object> row : storedLines) {
      Object id = row.get("supply_item_id");
      Object qty = row.get("requested_qty");
      if (id != null && qty != null) {
        lines.add(new ItemQty(((Number) id).longValue(), ((Number) qty).intValue()));
      }
    }
    return build(warehouseId, shelterId, eventId, lines);
  }

  /** 指纹不一致时给出人类可读的差异原因。 */
  public static String describeDifference(String incoming, String stored) {
    return "申请内容与原单不一致";
  }

  private IdempotencyFingerprint() {}
}
