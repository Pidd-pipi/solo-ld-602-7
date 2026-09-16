package com.generated.rescueStock.controllers;

import com.generated.rescueStock.routes.ShelterRoutes;
import com.generated.rescueStock.services.ShelterService;
import com.generated.rescueStock.types.ShelterPayload;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** 避难点维护与接收记录。 */
@RestController
@RequestMapping(ShelterRoutes.BASE)
public class ShelterController {
  private final ShelterService service;

  public ShelterController(ShelterService service) { this.service = service; }

  @GetMapping
  public List<Map<String, Object>> list() {
    return com.generated.rescueStock.constructors.ShelterViewFactory.list(service.list());
  }

  @PutMapping("/{id}")
  public Map<String, Object> update(@PathVariable long id, @Valid @RequestBody ShelterPayload payload) {
    ControllerSupport.run(() -> service.update(id, payload));
    return Map.of("updated", true);
  }

  @GetMapping("/{id}/receive-records")
  public List<Map<String, Object>> receiveRecords(@PathVariable long id) {
    return service.receiveRecords(id);
  }
}
