package com.generated.rescueStock.repositories;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 灾害事件数据访问。 */
@Repository
public class DisasterEventRepository {
  private final JdbcTemplate jdbc;

  public DisasterEventRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public List<Map<String, Object>> findAll() {
    return jdbc.queryForList(
        "SELECT e.*, COUNT(o.id) AS dispatch_count "
        + "FROM disaster_event e LEFT JOIN dispatch_order o ON o.event_id = e.id "
        + "GROUP BY e.id ORDER BY e.id DESC");
  }

  public Long create(String title, String district, String level) {
    jdbc.update("INSERT INTO disaster_event (title, district, level) VALUES (?,?,?)",
        title, district, level == null ? "MEDIUM" : level);
    return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
  }

  public boolean existsById(Long id) {
    if (id == null) return true;
    Integer c = jdbc.queryForObject("SELECT COUNT(1) FROM disaster_event WHERE id = ?", Integer.class, id);
    return c != null && c > 0;
  }
}
