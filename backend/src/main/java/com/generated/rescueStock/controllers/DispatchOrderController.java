package com.generated.rescueStock.controllers;

import com.generated.rescueStock.routes.DispatchOrderRoutes;
import com.generated.rescueStock.services.DispatchOrderService;
import com.generated.rescueStock.types.DispatchActionPayload;
import com.generated.rescueStock.types.DispatchApplyPayload;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 调拨闭环 REST 接口：申请 / 审批 / 出库 / 签收 / 拒签 / 撤销 + 列表 / 详情 / 批次去向。 */
@RestController
@RequestMapping(DispatchOrderRoutes.BASE)
public class DispatchOrderController {

  private final DispatchOrderService service;

  public DispatchOrderController(DispatchOrderService service) { this.service = service; }

  /** 调拨列表，可按状态、避难点过滤；数量随状态实时聚合。 */
  @GetMapping
  public List<Map<String, Object>> list(@RequestParam(required = false) String status,
                                        @RequestParam(required = false) Long shelterId) {
    return ControllerSupport.call(() -> service.list(status, shelterId));
  }

  /** 审批详情：主单 + 明细 + 批次去向 + 状态时间线。 */
  @GetMapping("/{id}")
  public Map<String, Object> detail(@PathVariable long id) {
    return ControllerSupport.call(() ->
        com.generated.rescueStock.constructors.DispatchOrderViewFactory.detail(
            service.detail(id), service.lines(id), service.allocations(id), service.timeline(id)));
  }

  @GetMapping("/{id}/lines")
  public List<Map<String, Object>> lines(@PathVariable long id) {
    return ControllerSupport.call(() -> service.lines(id));
  }

  @GetMapping("/{id}/allocations")
  public List<Map<String, Object>> allocations(@PathVariable long id) {
    return ControllerSupport.call(() -> service.allocations(id));
  }

  @GetMapping("/{id}/timeline")
  public List<Map<String, Object>> timeline(@PathVariable long id) {
    return ControllerSupport.call(() -> service.timeline(id));
  }

  /** 申请（先占可用库存）。重复 requestId 幂等返回原单，HTTP 200 + idempotent=true。 */
  @PostMapping
  @SuppressWarnings("unchecked")
  public ResponseEntity<Map<String, Object>> apply(@Valid @RequestBody DispatchApplyPayload payload) {
    Map<String, Object> order = ControllerSupport.call(() -> service.apply(payload));
    boolean idempotent = Boolean.TRUE.equals(order.get("_idempotent"));
    order.remove("_idempotent");
    return ResponseEntity.status(idempotent ? HttpStatus.OK : HttpStatus.CREATED).body(order);
  }

  @PostMapping("/{id}/approve")
  public Map<String, Object> approve(@PathVariable long id, @RequestBody(required = false) DispatchActionPayload body) {
    ControllerSupport.run(() -> service.approve(id, reason(body)));
    return ControllerSupport.created(false, id);
  }

  @PostMapping("/{id}/reject")
  public Map<String, Object> reject(@PathVariable long id, @RequestBody(required = false) DispatchActionPayload body) {
    ControllerSupport.run(() -> service.reject(id, reason(body)));
    return ControllerSupport.created(false, id);
  }

  /** 出库：按批次扣减并登记去向。 */
  @PostMapping("/{id}/outbound")
  public Map<String, Object> outbound(@PathVariable long id) {
    ControllerSupport.run(() -> service.outbound(id));
    return ControllerSupport.created(false, id);
  }

  @PostMapping("/{id}/receive")
  public Map<String, Object> receive(@PathVariable long id) {
    ControllerSupport.run(() -> service.receive(id));
    return ControllerSupport.created(false, id);
  }

  /** 拒签：原批次、原数量回补。 */
  @PostMapping("/{id}/refuse")
  public Map<String, Object> refuse(@PathVariable long id, @RequestBody(required = false) DispatchActionPayload body) {
    ControllerSupport.run(() -> service.refuse(id, reason(body)));
    return ControllerSupport.created(false, id);
  }

  /** 撤销：未出库释放占用，已出库原批次回补。 */
  @PostMapping("/{id}/cancel")
  public Map<String, Object> cancel(@PathVariable long id, @RequestBody(required = false) DispatchActionPayload body) {
    ControllerSupport.run(() -> service.cancel(id, reason(body)));
    return ControllerSupport.created(false, id);
  }

  private String reason(DispatchActionPayload body) {
    return body == null ? "" : body.reason;
  }
}
