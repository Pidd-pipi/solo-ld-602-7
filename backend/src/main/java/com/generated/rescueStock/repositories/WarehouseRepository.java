package com.generated.rescueStock.repositories;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 应急仓库数据访问。 */
@Repository
public class WarehouseRepository {
  private final JdbcTemplate jdbc;

  public WarehouseRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public List<Map<String, Object>> findAll() {
    return jdbc.queryForList(
        "SELECT w.*, u.display_name AS manager_name FROM warehouse w "
        + "LEFT JOIN app_user u ON u.id = w.manager_id ORDER BY w.id");
  }

  public Map<String, Object> findById(Long id) {
    List<Map<String, Object>> rows = jdbc.queryForList(
        "SELECT * FROM warehouse WHERE id = ?", id);
    return rows.isEmpty() ? null : rows.get(0);
  }

  public boolean existsById(Long id) {
    Integer c = jdbc.queryForObject("SELECT COUNT(1) FROM warehouse WHERE id = ?", Integer.class, id);
    return c != null && c > 0;
  }
}
