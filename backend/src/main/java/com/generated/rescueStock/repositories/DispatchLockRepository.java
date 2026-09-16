package com.generated.rescueStock.repositories;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 调拨单主表行锁补充：状态推进前先锁定订单行，杜绝两个动作并发踩踏。 */
@Repository
public class DispatchLockRepository {
  private final JdbcTemplate jdbc;

  public DispatchLockRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  /** 锁定订单行（仅事务内）。不存在返回空列表。 */
  public List<Long> lockOrder(Long orderId) {
    return jdbc.queryForList(
        "SELECT id FROM dispatch_order WHERE id = ? FOR UPDATE", Long.class, orderId);
  }
}
