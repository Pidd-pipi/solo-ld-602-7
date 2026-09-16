package com.generated.rescueStock.repositories;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 操作审计日志数据访问。 */
@Repository
public class AuditLogRepository {
  private final JdbcTemplate jdbc;

  public AuditLogRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public void append(String actor, String role, String action, String targetType,
                     String targetId, String detail, String result) {
    jdbc.update(
        "INSERT INTO audit_log (actor, role, action, target_type, target_id, detail, result) "
        + "VALUES (?,?,?,?,?,?,?)",
        actor == null ? "" : actor, role == null ? "" : role, action,
        targetType, targetId == null ? "" : targetId, detail == null ? "" : detail,
        result == null ? "SUCCESS" : result);
  }

  public List<Map<String, Object>> findRecent(int limit) {
    return jdbc.queryForList("SELECT * FROM audit_log ORDER BY id DESC LIMIT ?", limit);
  }
}
