package com.generated.rescueStock.services;

import com.generated.rescueStock.repositories.InventoryBatchRepository;
import com.generated.rescueStock.repositories.StockLedgerRepository;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** 态势大屏聚合：库存/占用/在途/待审批/临期等全部实时计算。 */
@Service
public class DashboardService {
  private final JdbcTemplate jdbc;
  private final InventoryBatchRepository batchRepo;
  private final StockLedgerRepository ledgerRepo;

  @Value("${rescue.ledger.near-expire-days:30}")
  private int nearExpireDays;

  public DashboardService(JdbcTemplate jdbc, InventoryBatchRepository batchRepo,
                          StockLedgerRepository ledgerRepo) {
    this.jdbc = jdbc;
    this.batchRepo = batchRepo;
    this.ledgerRepo = ledgerRepo;
  }

  public Map<String, Object> overview() {
    Map<String, Object> stock = jdbc.queryForMap(
        "SELECT COALESCE(SUM(quantity),0) AS total_qty, "
        + "COALESCE(SUM(held_quantity),0) AS held_qty, "
        + "COALESCE(SUM(quantity - held_quantity),0) AS available_qty, "
        + "COUNT(DISTINCT warehouse_id) AS warehouse_count, "
        + "COUNT(*) AS batch_count FROM inventory_batch");

    Map<String, Object> orders = jdbc.queryForMap(
        "SELECT "
        + "SUM(status='SUBMITTED') AS pending_approval, "
        + "SUM(status='APPROVED') AS approved_waiting_outbound, "
        + "SUM(status='DISPATCHED') AS in_transit, "
        + "SUM(status='RECEIVED') AS received, "
        + "SUM(status IN ('REJECTED','REFUSED','CANCELLED')) AS closed_abnormal, "
        + "COUNT(*) AS total_orders FROM dispatch_order");

    Integer nearExpire = jdbc.queryForObject(
        "SELECT COUNT(*) FROM inventory_batch WHERE expire_at IS NOT NULL "
        + "AND expire_at <= DATE_ADD(NOW(), INTERVAL ? DAY) AND quantity > 0",
        Integer.class, nearExpireDays);

    Integer sheltersOpen = jdbc.queryForObject(
        "SELECT COUNT(*) FROM shelter WHERE open_status IN ('OPEN','FULL')", Integer.class);

    return Map.of(
        "stock", stock,
        "orders", orders,
        "nearExpireBatches", nearExpire,
        "nearExpireDays", nearExpireDays,
        "sheltersOpen", sheltersOpen,
        "byItem", batchRepo.summarizeByItem(),
        "nearExpireList", nearExpireList(),
        "recentLedger", ledgerRepo.findRecent(10),
        "statusMatrix", statusMatrix());
  }

  /** 各状态调拨单数量，前端态势卡片/筛选器共用。 */
  public List<Map<String, Object>> statusMatrix() {
    return jdbc.queryForList(
        "SELECT status, COUNT(*) AS count FROM dispatch_order GROUP BY status ORDER BY status");
  }

  public List<Map<String, Object>> nearExpireList() {
    return jdbc.queryForList(
        "SELECT b.batch_no, b.quantity, b.held_quantity, b.expire_at, s.name AS supply_name, "
        + "s.unit, w.name AS warehouse_name, DATEDIFF(b.expire_at, NOW()) AS days_left "
        + "FROM inventory_batch b JOIN supply_item s ON s.id = b.supply_item_id "
        + "JOIN warehouse w ON w.id = b.warehouse_id "
        + "WHERE b.expire_at IS NOT NULL AND b.expire_at <= DATE_ADD(NOW(), INTERVAL ? DAY) "
        + "AND b.quantity > 0 ORDER BY b.expire_at ASC LIMIT 20", nearExpireDays);
  }
}
