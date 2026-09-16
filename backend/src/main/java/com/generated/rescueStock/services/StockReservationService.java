package com.generated.rescueStock.services;

import com.generated.rescueStock.constants.ErrorCodes;
import com.generated.rescueStock.constants.LedgerDirection;
import com.generated.rescueStock.repositories.AllocationRepository;
import com.generated.rescueStock.repositories.DispatchOrderRepository;
import com.generated.rescueStock.repositories.InventoryBatchRepository;
import com.generated.rescueStock.repositories.StockLedgerRepository;
import com.generated.rescueStock.repositories.SupplyItemRepository;
import com.generated.rescueStock.security.UserContextHolder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存占用/出库/回补的事务内核心算法。
 *
 * 不变量（任一不满足即整单回滚，绝不留下半占用或凭空回补）：
 *  - 0 &lt;= held_quantity &lt;= quantity
 *  - 占用/扣减只能来自“可用量 = quantity - held_quantity”
 *  - 回补量恒等于 allocation 记录里该单实际占/出过的数量
 */
@Service
public class StockReservationService {

  /** 申请口径的一行物资需求。 */
  public record RequestedLine(Long supplyItemId, int requestedQty) {}

  /** 占用落库后的一条批次去向。 */
  public record HeldAllocation(Long lineId, Long batchId, String batchNo, int qty) {}

  /** 锁定批次行的内存快照（写后即时推进，保证流水写后值真实）。 */
  static final class BatchSnap {
    final long batchId;
    final long supplyItemId;
    final long warehouseId;
    int quantity;
    int held;

    BatchSnap(long batchId, long supplyItemId, long warehouseId, int quantity, int held) {
      this.batchId = batchId;
      this.supplyItemId = supplyItemId;
      this.warehouseId = warehouseId;
      this.quantity = quantity;
      this.held = held;
    }
  }

  private final InventoryBatchRepository batchRepo;
  private final AllocationRepository allocationRepo;
  private final DispatchOrderRepository orderRepo;
  private final StockLedgerRepository ledgerRepo;
  private final SupplyItemRepository supplyItemRepo;

  public StockReservationService(InventoryBatchRepository batchRepo,
                                 AllocationRepository allocationRepo,
                                 DispatchOrderRepository orderRepo,
                                 StockLedgerRepository ledgerRepo,
                                 SupplyItemRepository supplyItemRepo) {
    this.batchRepo = batchRepo;
    this.allocationRepo = allocationRepo;
    this.orderRepo = orderRepo;
    this.ledgerRepo = ledgerRepo;
    this.supplyItemRepo = supplyItemRepo;
  }

  /**
   * 申请占用。
   *
   * 一致性要点：
   *  - 先把同一物资的多行合并、按物资 id 排序（与请求书写顺序无关）；
   *  - 用一条 SQL 一次性锁定本单所需全部物资的候选批次，且强制按批次主键 id 升序加锁，
   *    所有申请持锁顺序一致，明细顺序相反也不会交叉持锁（无死锁）；
   *  - 占用前先按锁定快照校验每种物资可用总量；不足则在写入任何数据之前抛错，整事务回滚；
   *  - 逐批 FEFO（临期优先）占用、登记去向、写 HOLD 流水。
   */
  @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
  public List<HeldAllocation> holdForOrder(Long orderId, long warehouseId, List<RequestedLine> lines) {
    String actor = UserContextHolder.actorName();
    List<HeldAllocation> result = new ArrayList<>();

    // 1) 归一化：同物资合并，按物资 id 升序
    java.util.TreeMap<Long, Integer> needByItem = new java.util.TreeMap<>();
    for (RequestedLine line : lines) {
      needByItem.merge(line.supplyItemId, line.requestedQty, Integer::sum);
    }

    // 2) 一次锁定所有候选批次（全局 batch id 升序），消除锁顺序反转
    List<Map<String, Object>> locked = batchRepo.lockAvailableByItems(warehouseId, new ArrayList<>(needByItem.keySet()));

    for (Map.Entry<Long, Integer> entry : needByItem.entrySet()) {
      long supplyItemId = entry.getKey();
      int requestedQty = entry.getValue();

      // 该物资的候选批次，在锁内按 FEFO（临期优先，其次批次 id）排序
      List<Map<String, Object>> batches = locked.stream()
          .filter(b -> ((Number) b.get("supply_item_id")).longValue() == supplyItemId)
          .sorted((x, y) -> {
            // null（无保质期）排最后；都有保质期则到期早的优先（FEFO）
            int c = Boolean.compare(expireIsNull(x), expireIsNull(y));
            if (c != 0) return c;
            Object ex = x.get("expire_at"), ey = y.get("expire_at");
            if (ex != null && ey != null) {
              int cmp = String.valueOf(ex).compareTo(String.valueOf(ey));
              if (cmp != 0) return cmp;
            }
            return Long.compare(((Number) x.get("id")).longValue(), ((Number) y.get("id")).longValue());
          })
          .toList();

      int availableTotal = batches.stream()
          .mapToInt(b -> toInt(b.get("quantity")) - toInt(b.get("held_quantity"))).sum();
      // 3) 占用前校验：不足直接抛错，此时本单尚未写任何行/去向/流水
      if (availableTotal < requestedQty) {
        throw BusinessException.of(ErrorCodes.INSUFFICIENT_STOCK, Map.of(
            "batchNo", "批次组", "itemName", itemName(supplyItemId),
            "need", requestedQty, "available", availableTotal));
      }

      int need = requestedQty;
      Long lineId = orderRepo.insertLine(orderId, supplyItemId, requestedQty);

      for (Map<String, Object> b : batches) {
        if (need == 0) {
          break;
        }
        long batchId = ((Number) b.get("id")).longValue();
        int quantity = toInt(b.get("quantity"));
        int held = toInt(b.get("held_quantity"));
        int available = quantity - held;
        if (available <= 0) {
          continue;
        }
        int take = Math.min(need, available);

        int updated = batchRepo.tryHold(batchId, take); // 条件占用，行锁之外的双保险
        if (updated != 1) {
          throw BusinessException.of(ErrorCodes.BATCH_CONTENDED,
              Map.of("batchNo", String.valueOf(b.get("batch_no"))));
        }
        allocationRepo.insert(orderId, lineId, batchId, take);
        ledgerRepo.append(batchId, supplyItemId, warehouseId, orderId,
            LedgerDirection.HOLD, take, quantity, held + take, actor,
            "申请占用 调拨单#" + orderId);

        result.add(new HeldAllocation(lineId, batchId, String.valueOf(b.get("batch_no")), take));
        need -= take;
      }
      if (need > 0) {
        // 锁内可用总量本应足够；走到这里说明被异常改写，整单回滚
        throw new BusinessException(ErrorCodes.CONCURRENT_CONFLICT);
      }
    }
    return result;
  }

  /** null 到期时间（无保质期）在 FEFO 中排在最后。 */
  private static boolean expireIsNull(Map<String, Object> b) {
    return b.get("expire_at") == null;
  }

  /**
   * 出库：按 allocation 原批次逐批扣减（quantity 与 held 同步减少），去向置 OUT，
   * 写 OUTBOUND 流水并回填调拨行出库量。任一批次条件更新失败即整单回滚。
   */
  @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
  public void outboundOrder(Long orderId, long warehouseId) {
    String actor = UserContextHolder.actorName();
    List<Map<String, Object>> allocs = allocationRepo.findByOrder(orderId);
    if (allocs.isEmpty()) {
      throw new BusinessException(ErrorCodes.ALLOCATION_MISSING, Map.of("orderId", orderId));
    }

    List<Long> batchIds = distinctBatchIds(allocs);
    Map<Long, BatchSnap> snap = lockSnapshot(batchRepo.lockByIds(batchIds));
    Map<Long, Integer> outboundByLine = new LinkedHashMap<>();

    for (Map<String, Object> a : allocs) {
      String stage = String.valueOf(a.get("stage"));
      int qty = toInt(a.get("allocated_qty")) - toInt(a.get("returned_qty"));
      if (qty <= 0) {
        continue;
      }
      if (!LedgerDirection.STAGE_HELD.equals(stage)) {
        // 只能从“占用中”出库，杜绝重复扣减
        throw new BusinessException(ErrorCodes.ILLEGAL_STATUS_TRANSITION, Map.of(
            "orderId", orderId, "from", stage, "action", "outbound", "to", "OUT"));
      }
      long allocId = ((Number) a.get("id")).longValue();
      long lineId = ((Number) a.get("dispatch_line_id")).longValue();
      long batchId = ((Number) a.get("inventory_batch_id")).longValue();
      BatchSnap cur = requireSnap(snap, batchId);
      if (cur.warehouseId != warehouseId) {
        throw new BusinessException(ErrorCodes.WAREHOUSE_MISMATCH,
            Map.of("batchNo", a.get("batch_no"), "warehouseId", warehouseId));
      }

      int updated = batchRepo.tryOutbound(batchId, qty);
      if (updated != 1) {
        throw new BusinessException(ErrorCodes.LEDGER_IMBALANCE, Map.of("batchId", batchId));
      }
      cur.quantity -= qty;
      cur.held -= qty;

      if (allocationRepo.markOut(allocId, qty) != 1) {
        throw new BusinessException(ErrorCodes.CONCURRENT_CONFLICT);
      }
      ledgerRepo.append(batchId, cur.supplyItemId, warehouseId, orderId,
          LedgerDirection.OUTBOUND, qty, cur.quantity, cur.held, actor,
          "出库扣减 调拨单#" + orderId + " 批次#" + a.get("batch_no"));
      outboundByLine.merge(lineId, qty, Integer::sum);
    }

    outboundByLine.forEach((lineId, qty) -> orderRepo.markLineOutbound(lineId, qty));
  }

  /**
   * 原批次回补/释放：对每张 allocation 按其当前阶段精确归还。
   *  HELD      -&gt; 释放占用（held -q），写 RELEASE
   *  OUT       -&gt; 物理回补（quantity +q），写 RETURN
   *  RETURNED  -&gt; 已回补，跳过，杜绝凭空/重复回补
   * 返回实际回补/释放的总数量。
   */
  @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
  public int restoreOrder(Long orderId, long warehouseId) {
    String actor = UserContextHolder.actorName();
    List<Map<String, Object>> allocs = allocationRepo.findByOrder(orderId);
    if (allocs.isEmpty()) {
      throw new BusinessException(ErrorCodes.ALLOCATION_MISSING, Map.of("orderId", orderId));
    }
    Map<Long, BatchSnap> snap = lockSnapshot(batchRepo.lockByIds(distinctBatchIds(allocs)));
    int restoredUnits = 0;

    for (Map<String, Object> a : allocs) {
      String stage = String.valueOf(a.get("stage"));
      int qty = toInt(a.get("allocated_qty")) - toInt(a.get("returned_qty"));
      if (qty <= 0 || LedgerDirection.STAGE_RETURNED.equals(stage)) {
        continue;
      }
      long allocId = ((Number) a.get("id")).longValue();
      long batchId = ((Number) a.get("inventory_batch_id")).longValue();
      BatchSnap cur = requireSnap(snap, batchId);

      String direction;
      int updated;
      if (LedgerDirection.STAGE_HELD.equals(stage)) {
        updated = batchRepo.tryReleaseHold(batchId, qty);
        cur.held -= qty;
        direction = LedgerDirection.RELEASE;
      } else { // OUT
        updated = batchRepo.returnToStock(batchId, qty);
        cur.quantity += qty;
        direction = LedgerDirection.RETURN;
      }
      if (updated != 1) {
        throw new BusinessException(ErrorCodes.LEDGER_IMBALANCE, Map.of("batchId", batchId));
      }
      if (allocationRepo.markReturned(allocId, qty) != 1) {
        throw new BusinessException(ErrorCodes.CONCURRENT_CONFLICT);
      }
      ledgerRepo.append(batchId, cur.supplyItemId, warehouseId, orderId,
          direction, qty, cur.quantity, cur.held, actor,
          "原批次回补 调拨单#" + orderId + " 批次#" + a.get("batch_no"));
      restoredUnits += qty;
    }
    return restoredUnits;
  }

  // ---- helpers ----

  private List<Long> distinctBatchIds(List<Map<String, Object>> allocs) {
    return allocs.stream()
        .map(a -> ((Number) a.get("inventory_batch_id")).longValue())
        .distinct().sorted().toList();
  }

  private Map<Long, BatchSnap> lockSnapshot(List<Map<String, Object>> lockedRows) {
    Map<Long, BatchSnap> snap = new HashMap<>();
    for (Map<String, Object> row : lockedRows) {
      snap.put(((Number) row.get("id")).longValue(), new BatchSnap(
          ((Number) row.get("id")).longValue(),
          ((Number) row.get("supply_item_id")).longValue(),
          ((Number) row.get("warehouse_id")).longValue(),
          toInt(row.get("quantity")),
          toInt(row.get("held_quantity"))));
    }
    return snap;
  }

  private BatchSnap requireSnap(Map<Long, BatchSnap> snap, long batchId) {
    BatchSnap cur = snap.get(batchId);
    if (cur == null) {
      throw new BusinessException(ErrorCodes.BATCH_NOT_FOUND, Map.of("batchId", batchId));
    }
    return cur;
  }

  private String itemName(Long supplyItemId) {
    Map<String, Object> item = supplyItemRepo.findById(supplyItemId);
    return item == null ? "物资#" + supplyItemId : String.valueOf(item.get("name"));
  }

  private static int toInt(Object o) {
    return o == null ? 0 : ((Number) o).intValue();
  }
}
