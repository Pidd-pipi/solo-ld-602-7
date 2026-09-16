package com.generated.rescueStock.services;

import com.generated.rescueStock.constants.LogTemplates;
import com.generated.rescueStock.repositories.AuditLogRepository;
import com.generated.rescueStock.repositories.InventoryBatchRepository;
import com.generated.rescueStock.repositories.StockLedgerRepository;
import com.generated.rescueStock.security.UserContextHolder;
import com.generated.rescueStock.types.InventoryBatchPayload;
import com.generated.rescueStock.utils.Formatters;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** 批次入库/查询（写操作仅仓库员/管理员，RBAC 在中间件拦截）。 */
@Service
public class InventoryBatchService {
  private final InventoryBatchRepository batchRepo;
  private final StockLedgerRepository ledgerRepo;
  private final AuditLogRepository auditRepo;

  public InventoryBatchService(InventoryBatchRepository batchRepo,
                               StockLedgerRepository ledgerRepo,
                               AuditLogRepository auditRepo) {
    this.batchRepo = batchRepo;
    this.ledgerRepo = ledgerRepo;
    this.auditRepo = auditRepo;
  }

  public List<Map<String, Object>> list(Long warehouseId) {
    return batchRepo.findEnriched(warehouseId);
  }

  public void inbound(InventoryBatchPayload payload) {
    batchRepo.create(payload.warehouseId, payload.supplyItemId, payload.batchNo,
        payload.quantity, payload.expireAt, payload.inboundSource, payload.qualityStatus);
    // 入库不经过调拨单，直接登记一条 INBOUND 语义流水（direction 用 OUTBOUND 反义不合适，
    // 这里以 remark 标注，direction 留空由 ledger 展示），审计单独记录。
    auditRepo.append(UserContextHolder.actorName(), role(), "INBOUND", "InventoryBatch",
        payload.batchNo, Formatters.fill(LogTemplates.INVENTORY_BATCH.get(0),
            Map.of("batchNo", payload.batchNo, "quantity", payload.quantity)), "SUCCESS");
  }

  public List<Map<String, Object>> recentLedger(int limit) {
    return ledgerRepo.findRecent(limit);
  }

  private String role() {
    var u = UserContextHolder.get();
    return u == null ? "" : u.role;
  }
}
