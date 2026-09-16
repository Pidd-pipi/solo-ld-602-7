package com.generated.rescueStock.constructors;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存批次响应构造器：追加唯一的派生量 available = quantity - held_quantity。
 * 可用量不在任何地方落地，避免与占用量出现两份事实。
 */
public final class InventoryBatchViewFactory {
  public static Map<String, Object> one(Map<String, Object> row) {
    int quantity = num(row.get("quantity"));
    int held = num(row.get("held_quantity"));
    Map<String, Object> view = new LinkedHashMap<>(row);
    view.put("available_quantity", quantity - held);
    return view;
  }

  public static List<Map<String, Object>> list(List<Map<String, Object>> rows) {
    return rows.stream().map(InventoryBatchViewFactory::one).toList();
  }

  private static int num(Object o) {
    return o == null ? 0 : ((Number) o).intValue();
  }

  private InventoryBatchViewFactory() {}
}
