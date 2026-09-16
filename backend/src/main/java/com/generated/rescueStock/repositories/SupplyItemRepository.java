package com.generated.rescueStock.repositories;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 应急物资档案数据访问。 */
@Repository
public class SupplyItemRepository {
  private final JdbcTemplate jdbc;

  public SupplyItemRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public List<Map<String, Object>> findAll() {
    return jdbc.queryForList("SELECT * FROM supply_item ORDER BY id");
  }

  public Map<String, Object> findById(Long id) {
    List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM supply_item WHERE id = ?", id);
    return rows.isEmpty() ? null : rows.get(0);
  }
}
