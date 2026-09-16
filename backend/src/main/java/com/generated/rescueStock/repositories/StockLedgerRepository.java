package com.generated.rescueStock.repositories;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 库存流水数据访问，所有库存变更同事务双写。 */
@Repository
public class StockLedgerRepository {
  private final JdbcTemplate jdbc;

  public StockLedgerRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public void append(Long batchId, Long supplyItemId, Long warehouseId, Long orderId,
                     String direction, int changeQty, int quantityAfter, int heldAfter,
                     String actor, String remark) {
    jdbc.update(
        "INSERT INTO stock_ledger (inventory_batch_id, supply_item_id, warehouse_id, dispatch_order_id, "
        + "direction, change_qty, quantity_after, held_after, actor, remark) "
        + "VALUES (?,?,?,?,?,?,?,?,?,?)",
        batchId, supplyItemId, warehouseId, orderId, direction, changeQty,
        quantityAfter, heldAfter, actor == null ? "" : actor, remark == null ? "" : remark);
  }

  public List<Map<String, Object>> findByOrder(Long orderId) {
    return jdbc.queryForList(
        "SELECT g.*, b.batch_no, s.name AS supply_name FROM stock_ledger g "
        + "JOIN inventory_batch b ON b.id = g.inventory_batch_id "
        + "JOIN supply_item s ON s.id = g.supply_item_id "
        + "WHERE g.dispatch_order_id = ? ORDER BY g.id", orderId);
  }

  public List<Map<String, Object>> findRecent(int limit) {
    return jdbc.queryForList(
        "SELECT g.*, b.batch_no, s.name AS supply_name FROM stock_ledger g "
        + "JOIN inventory_batch b ON b.id = g.inventory_batch_id "
        + "JOIN supply_item s ON s.id = g.supply_item_id "
        + "ORDER BY g.id DESC LIMIT ?", limit);
  }
}
