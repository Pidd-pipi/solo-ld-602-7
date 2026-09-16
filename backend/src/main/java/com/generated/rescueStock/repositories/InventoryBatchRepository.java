package com.generated.rescueStock.repositories;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 库存批次数据访问。
 *
 * 并发正确性双保险：
 *  1) 申请/出库/回补在事务内先以 id 升序 SELECT ... FOR UPDATE 锁定相关批次行，
 *     多个避难点争抢同一批次时在此串行化，且统一加锁顺序避免死锁；
 *  2) 真正的增减一律走“条件 UPDATE”，影响行数必须为 1，否则整单回滚，
 *     杜绝超占、超扣与凭空回补。
 */
@Repository
public class InventoryBatchRepository {
  private final JdbcTemplate jdbc;

  public InventoryBatchRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public List<Map<String, Object>> findEnriched(Long warehouseId) {
    String sql = "SELECT b.*, s.name AS supply_name, s.sku_code, s.category, s.unit, w.name AS warehouse_name "
        + "FROM inventory_batch b JOIN supply_item s ON s.id = b.supply_item_id "
        + "JOIN warehouse w ON w.id = b.warehouse_id "
        + (warehouseId != null ? "WHERE b.warehouse_id = ? " : "")
        + "ORDER BY b.warehouse_id, s.id, (b.expire_at IS NULL), b.expire_at, b.id";
    if (warehouseId != null) {
      return jdbc.queryForList(sql, warehouseId);
    }
    return jdbc.queryForList(sql);
  }

  /**
   * 一次性锁定一张单所需全部物资的候选批次，并强制按主键 id 升序加锁。
   *
   * 关键：所有申请都按同一全局顺序（batch id）拿行锁，与明细顺序无关，
   * 因此两张“相同物资、明细顺序相反”的并发申请不会出现交叉持锁，杜绝死锁。
   * FORCE INDEX(PRIMARY) 保证执行计划真的按主键顺序加锁。
   */
  public List<Map<String, Object>> lockAvailableByItems(Long warehouseId, List<Long> supplyItemIds) {
    if (supplyItemIds == null || supplyItemIds.isEmpty()) {
      return List.of();
    }
    String in = String.join(",", supplyItemIds.stream().map(x -> "?").toList());
    return jdbc.queryForList(
        "SELECT * FROM inventory_batch FORCE INDEX (PRIMARY) "
        + "WHERE warehouse_id = ? AND supply_item_id IN (" + in + ") "
        + "AND quantity - held_quantity > 0 AND quality_status <> 'DAMAGED' "
        + "ORDER BY id ASC FOR UPDATE",
        prepend(warehouseId, supplyItemIds));
  }

  /** 按 id 升序锁定指定批次（回补/出库复核用），同样强制主键顺序，仅在事务内调用。 */
  public List<Map<String, Object>> lockByIds(List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    String in = String.join(",", ids.stream().map(x -> "?").toList());
    return jdbc.queryForList(
        "SELECT * FROM inventory_batch FORCE INDEX (PRIMARY) WHERE id IN (" + in
        + ") ORDER BY id ASC FOR UPDATE",
        ids.toArray());
  }

  private Object[] prepend(Long first, List<Long> rest) {
    Object[] args = new Object[rest.size() + 1];
    args[0] = first;
    for (int i = 0; i < rest.size(); i++) {
      args[i + 1] = rest.get(i);
    }
    return args;
  }

  /** 条件占用：可用量足够才成功。返回影响行数（1=成功，0=被抢先/不足）。 */
  public int tryHold(Long batchId, int qty) {
    return jdbc.update(
        "UPDATE inventory_batch SET held_quantity = held_quantity + ? "
        + "WHERE id = ? AND quantity - held_quantity >= ?",
        qty, batchId, qty);
  }

  /** 条件出库扣减：同时从物理量与占用量中减掉已出库数量。 */
  public int tryOutbound(Long batchId, int qty) {
    return jdbc.update(
        "UPDATE inventory_batch SET quantity = quantity - ?, held_quantity = held_quantity - ? "
        + "WHERE id = ? AND quantity >= ? AND held_quantity >= ?",
        qty, qty, batchId, qty, qty);
  }

  /** 条件释放占用（审批驳回/未出库撤销）。 */
  public int tryReleaseHold(Long batchId, int qty) {
    return jdbc.update(
        "UPDATE inventory_batch SET held_quantity = held_quantity - ? "
        + "WHERE id = ? AND held_quantity >= ?",
        qty, batchId, qty);
  }

  /** 回补已出库物资（拒签/出库后撤销）：物理量加回，占用量不变。 */
  public int returnToStock(Long batchId, int qty) {
    return jdbc.update(
        "UPDATE inventory_batch SET quantity = quantity + ? WHERE id = ?",
        qty, batchId);
  }

  public int create(Long warehouseId, Long supplyItemId, String batchNo, int quantity,
                    String expireAt, String source, String quality) {
    return jdbc.update(
        "INSERT INTO inventory_batch (warehouse_id, supply_item_id, batch_no, quantity, held_quantity, "
        + "expire_at, inbound_source, quality_status) VALUES (?,?,?,?,0,?,?,?)",
        warehouseId, supplyItemId, batchNo, quantity,
        expireAt == null || expireAt.isBlank() ? null : expireAt,
        source == null ? "" : source, quality == null ? "OK" : quality);
  }

  /** 全库按物资汇总（态势大屏）。 */
  public List<Map<String, Object>> summarizeByItem() {
    return jdbc.queryForList(
        "SELECT s.id AS supply_item_id, s.name, s.sku_code, s.category, s.unit, s.safety_stock, "
        + "SUM(b.quantity) AS total_qty, SUM(b.held_quantity) AS held_qty, "
        + "SUM(b.quantity - b.held_quantity) AS available_qty "
        + "FROM supply_item s LEFT JOIN inventory_batch b ON b.supply_item_id = s.id "
        + "GROUP BY s.id ORDER BY s.id");
  }
}
