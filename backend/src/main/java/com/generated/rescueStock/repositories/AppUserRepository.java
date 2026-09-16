package com.generated.rescueStock.repositories;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 账号数据访问（JWT 登录）。 */
@Repository
public class AppUserRepository {
  private final JdbcTemplate jdbc;

  public AppUserRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public Map<String, Object> findByUsername(String username) {
    List<Map<String, Object>> rows =
        jdbc.queryForList("SELECT * FROM app_user WHERE username = ? AND status = 'ACTIVE'", username);
    return rows.isEmpty() ? null : rows.get(0);
  }

  public Map<String, Object> findById(Long id) {
    List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM app_user WHERE id = ?", id);
    return rows.isEmpty() ? null : rows.get(0);
  }
}
