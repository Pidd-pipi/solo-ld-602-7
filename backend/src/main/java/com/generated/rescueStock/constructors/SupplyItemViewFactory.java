package com.generated.rescueStock.constructors;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 物资档案响应构造器，附加分类中文展示。 */
public final class SupplyItemViewFactory {
  private static final Map<String, String> CATEGORY_TEXT = Map.of(
      "FOOD", "食品", "WATER", "饮水", "MEDICAL", "医疗",
      "SHELTER", "安置", "RESCUE_TOOL", "救援工具");

  public static Map<String, Object> one(Map<String, Object> row) {
    Map<String, Object> view = new LinkedHashMap<>(row);
    view.put("category_text", CATEGORY_TEXT.getOrDefault(String.valueOf(row.get("category")),
        String.valueOf(row.get("category"))));
    return view;
  }

  public static List<Map<String, Object>> list(List<Map<String, Object>> rows) {
    return rows.stream().map(SupplyItemViewFactory::one).toList();
  }

  private SupplyItemViewFactory() {}
}
