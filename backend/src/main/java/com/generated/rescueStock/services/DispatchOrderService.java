package com.generated.rescueStock.services;

import com.generated.rescueStock.constants.ErrorCodes;
import com.generated.rescueStock.repositories.DispatchOrderRepository;
import com.generated.rescueStock.repositories.DisasterEventRepository;
import com.generated.rescueStock.repositories.ShelterRepository;
import com.generated.rescueStock.repositories.WarehouseRepository;
import com.generated.rescueStock.types.DispatchApplyPayload;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 调拨应用服务：负责幂等判定、入参校验与读写编排；
 * 真正的库存与状态变更全部下沉到 {@link DispatchTxService} 的单事务中。
 */
@Service
public class DispatchOrderService {

  private final DispatchOrderRepository orderRepo;
  private final DispatchTxService tx;
  private final WarehouseRepository warehouseRepo;
  private final ShelterRepository shelterRepo;
  private final DisasterEventRepository eventRepo;
  private final com.generated.rescueStock.repositories.AllocationRepository allocationRepo;

  public DispatchOrderService(DispatchOrderRepository orderRepo,
                              DispatchTxService tx,
                              WarehouseRepository warehouseRepo,
                              ShelterRepository shelterRepo,
                              DisasterEventRepository eventRepo,
                              com.generated.rescueStock.repositories.AllocationRepository allocationRepo) {
    this.orderRepo = orderRepo;
    this.tx = tx;
    this.warehouseRepo = warehouseRepo;
    this.shelterRepo = shelterRepo;
    this.eventRepo = eventRepo;
    this.allocationRepo = allocationRepo;
  }

  public List<Map<String, Object>> list(String status, Long shelterId) {
    return orderRepo.findEnrichedList(status, shelterId);
  }

  public Map<String, Object> detail(Long orderId) {
    Map<String, Object> order = orderRepo.findById(orderId);
    if (order == null) {
      throw new BusinessException(ErrorCodes.ORDER_NOT_FOUND, Map.of("orderId", orderId));
    }
    return order;
  }

  public List<Map<String, Object>> lines(Long orderId) {
    return orderRepo.findLines(orderId);
  }

  public List<Map<String, Object>> allocations(Long orderId) {
    detail(orderId); // 不存在直接 404
    return allocationRepo.findByOrder(orderId);
  }

  public List<Map<String, Object>> timeline(Long orderId) {
    detail(orderId);
    return orderRepo.findTimeline(orderId);
  }

  /**
   * 申请调拨（幂等）。
   *
   * requestId 标识“同一次申请”，但是否真为同一申请还要看业务内容指纹：
   *  - 同 requestId 且内容一致（允许明细顺序不同、同物资多行合并后一致）-> 直接返回原单，不二次占库；
   *  - 同 requestId 但源仓库/避难点/事件/物资数量不同 -> 抛 IDEMPOTENT_CONFLICT，明确拒绝；
   *  - 同 requestId 并发撞唯一键：待赢家提交后读取并按同一规则判定一致或冲突。
   */
  public Map<String, Object> apply(DispatchApplyPayload payload) {
    validate(payload);
    String fingerprint = fingerprint(payload);

    Map<String, Object> existing = orderRepo.findByRequestId(payload.requestId);
    if (existing != null) {
      return resolveExisting(payload.requestId, fingerprint, existing);
    }

    Long userId = com.generated.rescueStock.security.UserContextHolder.userId();
    Long orderId;
    try {
      orderId = tx.createAndHold(payload, fingerprint, userId);
    } catch (DuplicateKeyException dup) {
      // 并发的同一 requestId 抢先插入：本次插入被唯一键拦住（库存动作在建单之后，尚未发生），
      // 等待赢家事务提交后回读，再按内容判定“幂等返回”还是“内容冲突”。
      Map<String, Object> raced = awaitByRequestId(payload.requestId, 50);
      if (raced == null) {
        // 极端情况下仍读不到，按并发冲突处理（本事务什么都没占）
        throw new BusinessException(ErrorCodes.CONCURRENT_CONFLICT);
      }
      return resolveExisting(payload.requestId, fingerprint, raced);
    }
    Map<String, Object> created = orderRepo.findById(orderId);
    created.put("_idempotent", false);
    return created;
  }

  /** 已存在同 requestId 单据：内容一致幂等返回，内容不同明确冲突。 */
  private Map<String, Object> resolveExisting(String requestId, String incomingFingerprint,
                                              Map<String, Object> stored) {
    String storedFingerprint = String.valueOf(stored.get("content_fingerprint"));
    if (!incomingFingerprint.equals(storedFingerprint)) {
      throw new BusinessException(ErrorCodes.IDEMPOTENT_CONFLICT, Map.of(
          "requestId", requestId,
          "orderId", stored.get("id"),
          "reason", com.generated.rescueStock.utils.IdempotencyFingerprint.describeDifference(
              incomingFingerprint, storedFingerprint)));
    }
    stored.put("_idempotent", true);
    return stored;
  }

  /** 归一化计算本次申请的内容指纹（与明细顺序无关，同物资多行合并）。 */
  private String fingerprint(DispatchApplyPayload payload) {
    List<com.generated.rescueStock.utils.IdempotencyFingerprint.ItemQty> items =
        payload.lines.stream()
            .map(l -> new com.generated.rescueStock.utils.IdempotencyFingerprint.ItemQty(
                l.supplyItemId, l.requestedQty))
            .toList();
    return com.generated.rescueStock.utils.IdempotencyFingerprint.build(
        payload.sourceWarehouseId, payload.shelterId, payload.eventId, items);
  }

  public void approve(long orderId, String reason) { tx.transit(orderId, "APPROVE", reason); }
  public void reject(long orderId, String reason) { tx.transit(orderId, "REJECT", reason); }
  public void outbound(long orderId) { tx.transit(orderId, "OUTBOUND", ""); }
  public void receive(long orderId) { tx.transit(orderId, "RECEIVE", ""); }
  public void refuse(long orderId, String reason) { tx.transit(orderId, "REFUSE", reason); }
  public void cancel(long orderId, String reason) { tx.transit(orderId, "CANCEL", reason); }

  /** 并发同 requestId 落库后，等待赢家事务提交并回读；超时返回 null。 */
  private Map<String, Object> awaitByRequestId(String requestId, int maxAttempts) {
    for (int i = 0; i < maxAttempts; i++) {
      Map<String, Object> row = orderRepo.findByRequestId(requestId);
      if (row != null) {
        return row;
      }
      try {
        Thread.sleep(20L);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return null;
      }
    }
    return null;
  }

  private void validate(DispatchApplyPayload payload) {    if (payload.requestId == null || payload.requestId.isBlank()) {
      throw new BusinessException(ErrorCodes.VALIDATION_FAILED,
          Map.of("reason", "requestId 不能为空（用于幂等）"));
    }
    if (payload.lines == null || payload.lines.isEmpty()) {
      throw new BusinessException(ErrorCodes.LINE_EMPTY, Map.of());
    }
    for (DispatchApplyPayload.Line line : payload.lines) {
      if (line.supplyItemId == null || line.requestedQty == null || line.requestedQty <= 0) {
        throw new BusinessException(ErrorCodes.VALIDATION_FAILED,
            Map.of("reason", "调拨明细物资与数量必须合法"));
      }
    }
    if (!warehouseRepo.existsById(payload.sourceWarehouseId)) {
      throw new BusinessException(ErrorCodes.VALIDATION_FAILED,
          Map.of("reason", "源仓库不存在"));
    }
    if (shelterRepo.findById(payload.shelterId) == null) {
      throw new BusinessException(ErrorCodes.VALIDATION_FAILED,
          Map.of("reason", "避难点不存在"));
    }
    if (!eventRepo.existsById(payload.eventId)) {
      throw new BusinessException(ErrorCodes.VALIDATION_FAILED,
          Map.of("reason", "灾害事件不存在"));
    }
  }
}
