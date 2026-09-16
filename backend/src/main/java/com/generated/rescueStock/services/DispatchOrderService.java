package com.generated.rescueStock.services;

import com.generated.rescueStock.constants.ErrorCodes;
import com.generated.rescueStock.repositories.DispatchLockRepository;
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
  private final DispatchLockRepository lockRepo;
  private final DispatchTxService tx;
  private final WarehouseRepository warehouseRepo;
  private final ShelterRepository shelterRepo;
  private final DisasterEventRepository eventRepo;
  private final com.generated.rescueStock.repositories.AllocationRepository allocationRepo;

  public DispatchOrderService(DispatchOrderRepository orderRepo,
                              DispatchLockRepository lockRepo,
                              DispatchTxService tx,
                              WarehouseRepository warehouseRepo,
                              ShelterRepository shelterRepo,
                              DisasterEventRepository eventRepo,
                              com.generated.rescueStock.repositories.AllocationRepository allocationRepo) {
    this.orderRepo = orderRepo;
    this.lockRepo = lockRepo;
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
   * 同一 requestId 的重复提交直接返回原单，绝不二次占库；
   * 即使两个相同请求并发，唯一键也只会放行一个，另一个回读原单。
   */
  public Map<String, Object> apply(DispatchApplyPayload payload) {
    validate(payload);

    Map<String, Object> existing = orderRepo.findByRequestId(payload.requestId);
    if (existing != null) {
      // 重复提交：原封不动返回，库存不再触碰
      existing.put("_idempotent", true);
      return existing;
    }

    Long userId = com.generated.rescueStock.security.UserContextHolder.userId();
    Long orderId;
    try {
      orderId = tx.createAndHold(payload, userId);
    } catch (DuplicateKeyException dup) {
      // 并发的同一申请抢先插入：唯一键拦住本次插入（库存动作在插入之后，故本次什么都没占）。
      // 等待赢家事务提交后回读原单，保证“不重复占库”。
      Map<String, Object> raced = awaitByRequestId(payload.requestId, 50);
      if (raced == null) {
        // 极端情况下仍读不到，按并发冲突处理（不会有任何占用残留）
        throw new BusinessException(ErrorCodes.CONCURRENT_CONFLICT);
      }
      raced.put("_idempotent", true);
      return raced;
    }
    Map<String, Object> created = orderRepo.findById(orderId);
    created.put("_idempotent", false);
    return created;
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
