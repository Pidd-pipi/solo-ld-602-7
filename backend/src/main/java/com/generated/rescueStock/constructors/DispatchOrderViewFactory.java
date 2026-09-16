package com.generated.rescueStock.constructors;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 调拨审批详情响应构造器：页面/控制器不得散写详情结构，统一由这里组装
 * 主单 + 申请明细 + 批次去向 + 状态时间线。新增字段必须改这里与前端类型。
 */
public final class DispatchOrderViewFactory {

  public static Map<String, Object> detail(Map<String, Object> order,
                                           List<Map<String, Object>> lines,
                                           List<Map<String, Object>> allocations,
                                           List<Map<String, Object>> timeline) {
    Map<String, Object> view = new LinkedHashMap<>(order);
    view.put("lines", lines);
    view.put("allocations", allocations);
    view.put("timeline", timeline);
    // 派生展示量：批次去向汇总，列表与详情口径一致
    int heldUnits = 0;
    int outUnits = 0;
    int returnedUnits = 0;
    for (Map<String, Object> a : allocations) {
      int allocated = toInt(a.get("allocated_qty"));
      int returned = toInt(a.get("returned_qty"));
      String stage = String.valueOf(a.get("stage"));
      switch (stage) {
        case "HELD" -> heldUnits += allocated - returned;
        case "OUT" -> outUnits += allocated - returned;
        default -> returnedUnits += allocated;
      }
    }
    view.put("heldUnits", heldUnits);
    view.put("outUnits", outUnits);
    view.put("returnedUnits", returnedUnits);
    return view;
  }

  private static int toInt(Object o) {
    return o == null ? 0 : ((Number) o).intValue();
  }

  private DispatchOrderViewFactory() {}
}
