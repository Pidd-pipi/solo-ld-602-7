package com.generated.rescueStock.controllers;

import com.generated.rescueStock.routes.DashboardRoutes;
import com.generated.rescueStock.services.DashboardService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 应急态势大屏聚合接口。 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
  private final DashboardService dashboard;

  public DashboardController(DashboardService dashboard) { this.dashboard = dashboard; }

  @GetMapping("/overview")
  public Map<String, Object> overview() {
    return dashboard.overview();
  }

  /** 状态数量矩阵，供大屏与列表筛选器共用，随状态实时变化。 */
  @GetMapping("/status-matrix")
  public List<Map<String, Object>> statusMatrix() {
    return dashboard.statusMatrix();
  }
}
