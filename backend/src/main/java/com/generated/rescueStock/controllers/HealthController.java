package com.generated.rescueStock.controllers;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 容器健康检查（compose healthcheck 依赖），无需鉴权。 */
@RestController
public class HealthController {
  @GetMapping("/health")
  public Map<String, Object> health() {
    return Map.of("status", "UP", "service", "rescue-stock-backend");
  }
}
