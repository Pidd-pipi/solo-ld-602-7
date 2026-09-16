export interface InventoryBatch {
  id: number;
  warehouse_id: number;
  supply_item_id: number;
  batch_no: string;
  quantity: number;
  held_quantity: number;
  expire_at: string | null;
  inbound_source: string;
  quality_status: string;
  supply_name?: string;
  sku_code?: string;
  category?: string;
  unit?: string;
  warehouse_name?: string;
}

/** 可用量始终实时计算，不存单独字段，避免与占用量不一致。 */
export const availableOf = (b: Pick<InventoryBatch, "quantity" | "held_quantity">): number =>
  b.quantity - b.held_quantity;
