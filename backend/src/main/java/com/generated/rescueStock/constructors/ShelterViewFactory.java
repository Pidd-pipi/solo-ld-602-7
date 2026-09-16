package com.generated.rescueStock.constructors;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 避难点响应构造器，附加入驻率百分比与开放状态文案。 */
public final class ShelterViewFactory {
  private static final Map<String, String> STATUS_TEXT = Map.of(
      "CLOSED", "关闭", "STANDBY", "待命", "OPEN", "开放", "FULL", "满载");

  public static Map<String, Object> one(Map<String, Object> row) {
    int capacity = num(row.get("capacity"));
    int population = num(row.get("current_population"));
    Map<String, Object> view = new LinkedHashMap<>(row);
    view.put("open_status_text", STATUS_TEXT.getOrDefault(String.valueOf(row.get("open_status")),
        String.valueOf(row.get("open_status"))));
    view.put("occupancy_percent", capacity <= 0 ? 0 : Math.min(100, Math.round(population * 100.0 / capacity)));
    return view;
  }

  public static List<Map<String, Object>> list(List<Map<String, Object>> rows) {
    return rows.stream().map(ShelterViewFactory::one).toList();
  }

  private static int num(Object o) {
    return o == null ? 0 : ((Number) o).intValue();
  }

  private ShelterViewFactory() {}
}
