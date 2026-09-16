package com.generated.rescueStock.repositories;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 避难点数据访问。 */
@Repository
public class ShelterRepository {
  private final JdbcTemplate jdbc;

  public ShelterRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public List<Map<String, Object>> findAll() {
    return jdbc.queryForList("SELECT * FROM shelter ORDER BY id");
  }

  public Map<String, Object> findById(Long id) {
    List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM shelter WHERE id = ?", id);
    return rows.isEmpty() ? null : rows.get(0);
  }

  public void update(Long id, Integer capacity, Integer population, String contactPerson,
                     String contactPhone, String riskLevel, String openStatus) {
    jdbc.update("UPDATE shelter SET capacity=?, current_population=?, contact_person=?, "
        + "contact_phone=?, risk_level=?, open_status=? WHERE id=?",
        capacity, population, contactPerson, contactPhone, riskLevel, openStatus, id);
  }
}
