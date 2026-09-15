import type { InventoryBatch } from "../types/InventoryBatch";

export const createDefaultInventoryBatch = (overrides: Partial<InventoryBatch> = {}): InventoryBatch => ({
  id: 1 as never,
  warehouse_id: 1 as never,
  supply_item_id: 1 as never,
  batch_no: "batch no 1" as never,
  quantity: 92 as never,
  expire_at: "2026-06-11T09:00:00Z" as never,
  inbound_source: "inbound source 1" as never,
  quality_status: "SUBMITTED" as never,
  ...overrides
});

export const createInventoryBatchForm = createDefaultInventoryBatch;
export const createInventoryBatchResponse = createDefaultInventoryBatch;
