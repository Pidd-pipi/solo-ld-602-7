package com.generated.rescueStock.controllers;

import com.generated.rescueStock.routes.InventoryBatchRoutes;
import com.generated.rescueStock.services.InventoryBatchService;
import com.generated.rescueStock.types.InventoryBatchPayload;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 库存批次查询 / 入库。 */
@RestController
@RequestMapping(InventoryBatchRoutes.BASE)
public class InventoryBatchController {
  private final InventoryBatchService service;

  public InventoryBatchController(InventoryBatchService service) { this.service = service; }

  @GetMapping
  public List<Map<String, Object>> list(@RequestParam(required = false) Long warehouseId) {
    return com.generated.rescueStock.constructors.InventoryBatchViewFactory.list(service.list(warehouseId));
  }

  @GetMapping("/ledger")
  public List<Map<String, Object>> ledger(@RequestParam(defaultValue = "50") int limit) {
    return service.recentLedger(Math.min(limit, 200));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> inbound(@Valid @RequestBody InventoryBatchPayload payload) {
    ControllerSupport.run(() -> service.inbound(payload));
    return Map.of("created", true);
  }
}
