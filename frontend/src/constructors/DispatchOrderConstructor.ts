import type { DispatchOrder } from "../types/DispatchOrder";

export const createDefaultDispatchOrder = (overrides: Partial<DispatchOrder> = {}): DispatchOrder => ({
  id: 1 as never,
  event_id: 1 as never,
  source_warehouse_id: 1 as never,
  shelter_id: 1 as never,
  priority: "priority 1" as never,
  status: "SUBMITTED" as never,
  requested_by: "requested by 1" as never,
  approved_by: "approved by 1" as never,
  dispatched_at: "2026-06-11T09:00:00Z" as never,
  ...overrides
});

export const createDispatchOrderForm = createDefaultDispatchOrder;
export const createDispatchOrderResponse = createDefaultDispatchOrder;
