export interface InventoryBatch {
  id: number;
  warehouse_id: number;
  supply_item_id: number;
  batch_no: string;
  quantity: number;
  expire_at: string;
  inbound_source: string;
  quality_status: string;
}
