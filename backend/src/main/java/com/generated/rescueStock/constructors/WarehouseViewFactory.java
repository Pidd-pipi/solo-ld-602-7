package com.generated.rescueStock.constructors;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 仓库响应构造器：统一列表/单条视图结构，控制器不散写。 */
public final class WarehouseViewFactory {
  public static Map<String, Object> one(Map<String, Object> row) {
    Map<String, Object> view = new LinkedHashMap<>(row);
    view.putIfAbsent("status_text", "ACTIVE".equals(String.valueOf(row.get("status"))) ? "启用" : "停用");
    return view;
  }

  public static List<Map<String, Object>> list(List<Map<String, Object>> rows) {
    return rows.stream().map(WarehouseViewFactory::one).toList();
  }

  private WarehouseViewFactory() {}
}
