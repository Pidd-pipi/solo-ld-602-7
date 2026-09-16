import type { LedgerDirection } from "../constants/LedgerDirection";

export interface StockLedgerEntry {
  id: number;
  inventory_batch_id: number;
  supply_item_id: number;
  warehouse_id: number;
  dispatch_order_id: number | null;
  direction: LedgerDirection;
  change_qty: number;
  quantity_after: number;
  held_after: number;
  actor: string;
  remark: string;
  created_at: string;
  batch_no?: string;
  supply_name?: string;
}
