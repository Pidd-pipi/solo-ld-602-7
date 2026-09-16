package com.generated.rescueStock.services;

import com.generated.rescueStock.constants.DispatchStatus;
import com.generated.rescueStock.constants.ErrorCodes;
import com.generated.rescueStock.constants.LogTemplates;
import com.generated.rescueStock.repositories.AuditLogRepository;
import com.generated.rescueStock.repositories.DispatchLockRepository;
import com.generated.rescueStock.repositories.DispatchOrderRepository;
import com.generated.rescueStock.security.CurrentUser;
import com.generated.rescueStock.security.UserContextHolder;
import com.generated.rescueStock.utils.Formatters;
import com.generated.rescueStock.types.DispatchApplyPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 调拨闭环的事务边界：申请占库与每次状态推进都在单个事务内完成，
 * 库存动作 + 状态 CAS + 时间线 + 审计同提交或同回滚。
 */
@Service
public class DispatchTxService {

  private final DispatchOrderRepository orderRepo;
  private final DispatchLockRepository lockRepo;
  private final StockReservationService reservation;
  private final AuditLogRepository auditRepo;

  public DispatchTxService(DispatchOrderRepository orderRepo,
                           DispatchLockRepository lockRepo,
                           StockReservationService reservation,
                           AuditLogRepository auditRepo) {
    this.orderRepo = orderRepo;
    this.lockRepo = lockRepo;
    this.reservation = reservation;
    this.auditRepo = auditRepo;
  }

  /** 申请：建单（SUBMITTED）并按批次占用库存。requestId 唯一键是幂等的最后防线。 */
  @Transactional(rollbackFor = Exception.class)
  public Long createAndHold(DispatchApplyPayload payload, Long userId) {
    Long orderId = orderRepo.insert(payload.requestId, payload.eventId,
        payload.sourceWarehouseId, payload.shelterId, payload.priority, payload.remark, userId);

    List<StockReservationService.RequestedLine> lines = new ArrayList<>();
    for (DispatchApplyPayload.Line line : payload.lines) {
      lines.add(new StockReservationService.RequestedLine(line.supplyItemId, line.requestedQty));
    }
    reservation.holdForOrder(orderId, payload.sourceWarehouseId, lines);

    orderRepo.insertStatusLog(orderId, "SUBMIT", "", DispatchStatus.SUBMITTED.name(),
        UserContextHolder.actorName(), "提交申请并占用可用库存");
    audit(UserContextHolder.get(), "SUBMIT", orderId, Formatters.fill(
        LogTemplates.DISPATCH_SUBMIT, Map.of("orderId", orderId, "lineCount", lines.size())));
    return orderId;
  }

  /**
   * 状态推进通用事务：锁订单行 -&gt; 校验状态机 -&gt; 执行库存动作 -&gt; CAS 状态 -&gt; 写时间线/审计。
   * 库存动作失败或 CAS 影响 0 行都会整体回滚。
   */
  @Transactional(rollbackFor = Exception.class)
  public void transit(long orderId, String action, String reason) {
    CurrentUser actor = UserContextHolder.get();
    String actorName = actor == null ? "system" : actor.username;
    Long actorId = actor == null ? null : actor.id;

    if (lockRepo.lockOrder(orderId).isEmpty()) {
      throw new BusinessException(ErrorCodes.ORDER_NOT_FOUND, Map.of("orderId", orderId));
    }
    Map<String, Object> order = orderRepo.findById(orderId);
    DispatchStatus from = DispatchStatus.valueOf(String.valueOf(order.get("status")));
    long warehouseId = ((Number) order.get("source_warehouse_id")).longValue();

    DispatchStatus target;
    List<String> sets = new ArrayList<>();
    List<Object> args = new ArrayList<>();
    String logTemplate;
    String note = reason == null ? "" : reason;

    switch (action) {
      case "APPROVE" -> {
        target = DispatchStatus.APPROVED;
        sets.add("approved_by = ?"); args.add(actorId);
        sets.add("approved_at = NOW()");
        logTemplate = LogTemplates.DISPATCH_APPROVE;
      }
      case "REJECT" -> {
        target = DispatchStatus.REJECTED;
        sets.add("reject_reason = ?"); args.add(note);
        logTemplate = LogTemplates.DISPATCH_REJECT;
      }
      case "OUTBOUND" -> {
        target = DispatchStatus.DISPATCHED;
        sets.add("dispatched_at = NOW()");
        logTemplate = LogTemplates.DISPATCH_OUTBOUND;
      }
      case "RECEIVE" -> {
        target = DispatchStatus.RECEIVED;
        sets.add("received_at = NOW()");
        logTemplate = LogTemplates.DISPATCH_RECEIVE;
      }
      case "REFUSE" -> {
        target = DispatchStatus.REFUSED;
        sets.add("refused_by = ?"); args.add(actorId);
        sets.add("refuse_reason = ?"); args.add(note);
        sets.add("refused_at = NOW()");
        logTemplate = LogTemplates.DISPATCH_REFUSE;
      }
      case "CANCEL" -> {
        target = DispatchStatus.CANCELLED;
        sets.add("canceled_by = ?"); args.add(actorId);
        sets.add("cancel_reason = ?"); args.add(note);
        sets.add("canceled_at = NOW()");
        logTemplate = LogTemplates.DISPATCH_CANCEL;
      }
      default -> throw new BusinessException(ErrorCodes.VALIDATION_FAILED,
          Map.of("reason", "未知动作 " + action));
    }

    if (!from.canTransitTo(target)) {
      throw new BusinessException(ErrorCodes.ILLEGAL_STATUS_TRANSITION, Map.of(
          "orderId", orderId, "from", from.name(), "action", action, "to", target.name()));
    }

    // 库存动作必须先于状态 CAS：失败则整单回滚，状态与库存永不脱节
    int allocCount = 0;
    switch (target) {
      // 驳回/撤销释放占用；拒签发生在出库之后，按原批次做物理回补
      case REJECTED, REFUSED, CANCELLED -> reservation.restoreOrder(orderId, warehouseId);
      case DISPATCHED -> {
        reservation.outboundOrder(orderId, warehouseId);
        allocCount = 1;
      }
      default -> { /* APPROVED / RECEIVED 不改变库存 */ }
    }

    int changed = orderRepo.compareAndSetStatus(orderId, from.name(), target.name(), sets, args);
    if (changed != 1) {
      // 订单在锁内仍被改写（理论不会），显式回滚
      throw new BusinessException(ErrorCodes.CONCURRENT_CONFLICT);
    }

    orderRepo.insertStatusLog(orderId, action, from.name(), target.name(), actorName, note);
    audit(actor, action, orderId, Formatters.fill(logTemplate, Map.of(
        "orderId", orderId, "reason", note, "allocCount", allocCount)));
  }

  private void audit(CurrentUser actor, String action, long orderId, String detail) {
    auditRepo.append(actor == null ? "system" : actor.username,
        actor == null ? "" : actor.role, action, "DispatchOrder",
        String.valueOf(orderId), detail, "SUCCESS");
  }
}
