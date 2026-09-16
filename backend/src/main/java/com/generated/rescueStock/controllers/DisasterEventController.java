package com.generated.rescueStock.controllers;

import com.generated.rescueStock.routes.DisasterEventRoutes;
import com.generated.rescueStock.services.DisasterEventService;
import com.generated.rescueStock.types.DisasterEventPayload;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 灾害事件登记与列表。 */
@RestController
@RequestMapping(DisasterEventRoutes.BASE)
public class DisasterEventController {
  private final DisasterEventService service;

  public DisasterEventController(DisasterEventService service) { this.service = service; }

  @GetMapping
  public List<Map<String, Object>> list() {
    return service.list();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> create(@Valid @RequestBody DisasterEventPayload payload) {
    Long id = service.create(payload);
    return Map.of("id", id);
  }
}
