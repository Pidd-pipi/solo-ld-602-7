package com.generated.rescueStock.services;

import com.generated.rescueStock.constants.LogTemplates;
import com.generated.rescueStock.repositories.AuditLogRepository;
import com.generated.rescueStock.repositories.ShelterRepository;
import com.generated.rescueStock.security.UserContextHolder;
import com.generated.rescueStock.types.ShelterPayload;
import com.generated.rescueStock.utils.Formatters;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** 避难点维护 + 接收记录（含单调拨）查询。 */
@Service
public class ShelterService {
  private final ShelterRepository shelterRepo;
  private final AuditLogRepository auditRepo;
  private final JdbcTemplate jdbc;

  public ShelterService(ShelterRepository shelterRepo, AuditLogRepository auditRepo, JdbcTemplate jdbc) {
    this.shelterRepo = shelterRepo;
    this.auditRepo = auditRepo;
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> list() {
    return jdbc.queryForList(
        "SELECT s.*, "
        + "SUM(o.status='DISPATCHED') AS inbound_in_transit, "
        + "SUM(o.status='RECEIVED') AS received_orders "
        + "FROM shelter s LEFT JOIN dispatch_order o ON o.shelter_id = s.id "
        + "GROUP BY s.id ORDER BY s.id");
  }

  public void update(Long id, ShelterPayload payload) {
    shelterRepo.update(id, payload.capacity,
        payload.currentPopulation == null ? 0 : payload.currentPopulation,
        payload.contactPerson, payload.contactPhone, payload.riskLevel, payload.openStatus);
    auditRepo.append(UserContextHolder.actorName(), role(), "UPDATE", "Shelter",
        String.valueOf(id),
        Formatters.fill(LogTemplates.SHELTER.get(2),
            Map.of("name", payload.name, "openStatus", payload.openStatus)), "SUCCESS");
  }

  /** 某避难点的调拨接收记录（态势随状态实时变化）。 */
  public List<Map<String, Object>> receiveRecords(Long shelterId) {
    return jdbc.queryForList(
        "SELECT o.id, o.status, o.priority, o.dispatched_at, o.received_at, "
        + "w.name AS warehouse_name, (SELECT COALESCE(SUM(l.requested_qty),0) "
        + "FROM dispatch_line l WHERE l.dispatch_order_id = o.id) AS total_requested "
        + "FROM dispatch_order o JOIN warehouse w ON w.id = o.source_warehouse_id "
        + "WHERE o.shelter_id = ? ORDER BY o.id DESC", shelterId);
  }

  private String role() {
    var u = UserContextHolder.get();
    return u == null ? "" : u.role;
  }
}
