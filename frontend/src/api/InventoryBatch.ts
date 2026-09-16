import { request } from "../utils/request";
import type { InventoryBatch } from "../types/InventoryBatch";
import type { StockLedgerEntry } from "../types/StockLedger";

export const listBatches = (warehouseId?: number): Promise<InventoryBatch[]> => {
  const suffix = warehouseId ? `?warehouseId=${warehouseId}` : "";
  return request<InventoryBatch[]>(`/api/batches${suffix}`);
};

export const listLedger = (limit = 50): Promise<StockLedgerEntry[]> =>
  request<StockLedgerEntry[]>(`/api/batches/ledger?limit=${limit}`);
