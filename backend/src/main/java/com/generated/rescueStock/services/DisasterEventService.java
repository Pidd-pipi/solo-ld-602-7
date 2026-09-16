package com.generated.rescueStock.services;

import com.generated.rescueStock.repositories.DisasterEventRepository;
import com.generated.rescueStock.types.DisasterEventPayload;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** 灾害事件登记与列表。 */
@Service
public class DisasterEventService {
  private final DisasterEventRepository repo;

  public DisasterEventService(DisasterEventRepository repo) { this.repo = repo; }

  public List<Map<String, Object>> list() { return repo.findAll(); }

  public Long create(DisasterEventPayload payload) {
    return repo.create(payload.title, payload.district, payload.level);
  }
}
