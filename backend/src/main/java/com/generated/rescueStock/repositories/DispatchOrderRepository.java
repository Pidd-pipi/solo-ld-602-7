package com.generated.rescueStock.repositories;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 调拨单主表 + 调拨行 + 状态时间线数据访问。 */
@Repository
public class DispatchOrderRepository {
  private final JdbcTemplate jdbc;

  public DispatchOrderRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public Long insert(String requestId, String contentFingerprint, Long eventId, Long warehouseId,
                     Long shelterId, String priority, String remark, Long requestedBy) {
    jdbc.update(
        "INSERT INTO dispatch_order (request_id, content_fingerprint, event_id, source_warehouse_id, shelter_id, "
        + "priority, status, remark, requested_by) VALUES (?,?,?,?,?,?,?,?,?)",
        requestId, contentFingerprint == null ? "" : contentFingerprint,
        eventId, warehouseId, shelterId,
        priority == null ? "NORMAL" : priority, "SUBMITTED",
        remark == null ? "" : remark, requestedBy);
    return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
  }

  public Map<String, Object> findById(Long id) {
    List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM dispatch_order WHERE id = ?", id);
    return rows.isEmpty() ? null : rows.get(0);
  }

  /** 幂等键查询：同一 requestId 直接命中原单。 */
  public Map<String, Object> findByRequestId(String requestId) {
    if (requestId == null) return null;
    List<Map<String, Object>> rows =
        jdbc.queryForList("SELECT * FROM dispatch_order WHERE request_id = ?", requestId);
    return rows.isEmpty() ? null : rows.get(0);
  }

  /** 列表：联仓库/避难点/事件/申请人，并汇总行数与申请/出库数量。 */
  public List<Map<String, Object>> findEnrichedList(String status, Long shelterId) {
    StringBuilder sql = new StringBuilder(
        "SELECT o.*, w.name AS warehouse_name, sh.name AS shelter_name, sh.district AS shelter_district, "
        + "e.title AS event_title, u.display_name AS requester_name, "
        + "COUNT(DISTINCT l.id) AS line_count, "
        + "COALESCE(SUM(l.requested_qty),0) AS total_requested, "
        + "COALESCE(SUM(l.outbound_qty),0) AS total_outbound "
        + "FROM dispatch_order o "
        + "JOIN warehouse w ON w.id = o.source_warehouse_id "
        + "JOIN shelter sh ON sh.id = o.shelter_id "
        + "LEFT JOIN disaster_event e ON e.id = o.event_id "
        + "LEFT JOIN app_user u ON u.id = o.requested_by "
        + "LEFT JOIN dispatch_line l ON l.dispatch_order_id = o.id WHERE 1=1 ");
    List<Object> args = new java.util.ArrayList<>();
    if (status != null && !status.isBlank()) {
      sql.append("AND o.status = ? ");
      args.add(status);
    }
    if (shelterId != null) {
      sql.append("AND o.shelter_id = ? ");
      args.add(shelterId);
    }
    sql.append("GROUP BY o.id ORDER BY o.id DESC");
    return jdbc.queryForList(sql.toString(), args.toArray());
  }

  public Long insertLine(Long orderId, Long supplyItemId, int requestedQty) {
    jdbc.update("INSERT INTO dispatch_line (dispatch_order_id, supply_item_id, requested_qty) VALUES (?,?,?)",
        orderId, supplyItemId, requestedQty);
    return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
  }

  public List<Map<String, Object>> findLines(Long orderId) {
    return jdbc.queryForList(
        "SELECT l.*, s.name AS supply_name, s.sku_code, s.category, s.unit "
        + "FROM dispatch_line l JOIN supply_item s ON s.id = l.supply_item_id "
        + "WHERE l.dispatch_order_id = ? ORDER BY l.id", orderId);
  }

  /**
   * 状态机条件更新：仅当当前状态等于期望值时才推进，并顺带写入时间戳/操作人/原因列。
   * 影响行数 0 即并发冲突或非法迁移，service 据此回滚并报错——不允许任何半推进。
   */
  public int compareAndSetStatus(Long id, String expect, String target,
                                 java.util.List<String> extraSets, java.util.List<Object> extraArgs) {
    StringBuilder sql = new StringBuilder("UPDATE dispatch_order SET status = ?");
    List<Object> args = new java.util.ArrayList<>();
    args.add(target);
    if (extraSets != null) {
      for (String set : extraSets) {
        sql.append(", ").append(set);
      }
      args.addAll(extraArgs);
    }
    sql.append(" WHERE id = ? AND status = ?");
    args.add(id);
    args.add(expect);
    return jdbc.update(sql.toString(), args.toArray());
  }

  public void markLineOutbound(Long lineId, int outboundQty) {
    jdbc.update("UPDATE dispatch_line SET outbound_qty = ? WHERE id = ?", outboundQty, lineId);
  }

  // ---- 状态时间线 ----
  public void insertStatusLog(Long orderId, String action, String from, String to, String actor, String note) {
    jdbc.update(
        "INSERT INTO dispatch_status_log (dispatch_order_id, action, from_status, to_status, actor, note) "
        + "VALUES (?,?,?,?,?,?)", orderId, action, from == null ? "" : from, to,
        actor == null ? "" : actor, note == null ? "" : note);
  }

  public List<Map<String, Object>> findTimeline(Long orderId) {
    return jdbc.queryForList(
        "SELECT * FROM dispatch_status_log WHERE dispatch_order_id = ? ORDER BY id", orderId);
  }
}
