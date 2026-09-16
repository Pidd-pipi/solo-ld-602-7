package com.generated.rescueStock.repositories;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 批次去向（dispatch_batch_allocation）数据访问。 */
@Repository
public class AllocationRepository {
  private final JdbcTemplate jdbc;

  public AllocationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public Long insert(Long orderId, Long lineId, Long batchId, int allocatedQty) {
    jdbc.update(
        "INSERT INTO dispatch_batch_allocation (dispatch_order_id, dispatch_line_id, "
        + "inventory_batch_id, allocated_qty, returned_qty, stage) VALUES (?,?,?,?,0,'HELD')",
        orderId, lineId, batchId, allocatedQty);
    return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
  }

  /** 单调拨单的全部批次去向，按行、批次排序，供出库与详情使用。 */
  public List<Map<String, Object>> findByOrder(Long orderId) {
    return jdbc.queryForList(
        "SELECT a.*, b.batch_no, b.warehouse_id, s.name AS supply_name, s.unit, w.name AS warehouse_name "
        + "FROM dispatch_batch_allocation a "
        + "JOIN inventory_batch b ON b.id = a.inventory_batch_id "
        + "JOIN supply_item s ON s.id = b.supply_item_id "
        + "JOIN warehouse w ON w.id = b.warehouse_id "
        + "WHERE a.dispatch_order_id = ? ORDER BY a.dispatch_line_id, a.id", orderId);
  }

  /** HELD -> OUT：出库时把占用阶段置为已出库。 */
  public int markOut(Long allocationId, int qty) {
    return jdbc.update(
        "UPDATE dispatch_batch_allocation SET stage = 'OUT' WHERE id = ? AND stage = 'HELD' "
        + "AND allocated_qty - returned_qty >= ?", allocationId, qty);
  }

  /** 记录回补量并把阶段置为 RETURNED（拒签/撤销）。 */
  public int markReturned(Long allocationId, int qty) {
    return jdbc.update(
        "UPDATE dispatch_batch_allocation SET returned_qty = returned_qty + ?, stage = 'RETURNED' "
        + "WHERE id = ? AND returned_qty + ? <= allocated_qty", qty, allocationId, qty);
  }
}
