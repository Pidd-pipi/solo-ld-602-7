import type { InventoryBatch } from "./InventoryBatch";
import type { StockLedgerEntry } from "./StockLedger";

export interface StockSummary {
  total_qty: number;
  held_qty: number;
  available_qty: number;
  warehouse_count: number;
  batch_count: number;
}

export interface OrderSummary {
  pending_approval: number;
  approved_waiting_outbound: number;
  in_transit: number;
  received: number;
  closed_abnormal: number;
  total_orders: number;
}

export interface ItemSummary {
  supply_item_id: number;
  name: string;
  sku_code: string;
  category: string;
  unit: string;
  safety_stock: number;
  total_qty: number;
  held_qty: number;
  available_qty: number;
}

export interface NearExpireRow {
  batch_no: string;
  quantity: number;
  held_quantity: number;
  expire_at: string;
  supply_name: string;
  unit: string;
  warehouse_name: string;
  days_left: number;
}

export interface DashboardOverview {
  stock: StockSummary;
  orders: OrderSummary;
  nearExpireBatches: number;
  nearExpireDays: number;
  sheltersOpen: number;
  byItem: ItemSummary[];
  nearExpireList: NearExpireRow[];
  recentLedger: StockLedgerEntry[];
  statusMatrix: { status: string; count: number }[];
}

export type { InventoryBatch, StockLedgerEntry };
