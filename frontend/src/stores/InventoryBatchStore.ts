import { defineStore } from "pinia";
import { listBatches } from "../api/InventoryBatch";
import type { InventoryBatch } from "../types/InventoryBatch";
import { availableOf } from "../types/InventoryBatch";

/** 批次库存：available 实时由 quantity - held_quantity 派生。 */
export const useInventoryBatchStore = defineStore("inventoryBatch", {
  state: () => ({
    rows: [] as InventoryBatch[],
    warehouseFilter: 0,
    loading: false
  }),
  getters: {
    filtered(state): InventoryBatch[] {
      return state.warehouseFilter
        ? state.rows.filter((b) => b.warehouse_id === state.warehouseFilter)
        : state.rows;
    },
    totalQuantity(): number {
      return this.filtered.reduce((s, b) => s + b.quantity, 0);
    },
    totalHeld(): number {
      return this.filtered.reduce((s, b) => s + b.held_quantity, 0);
    },
    totalAvailable(): number {
      return this.filtered.reduce((s, b) => s + availableOf(b), 0);
    }
  },
  actions: {
    async load() {
      this.loading = true;
      try {
        this.rows = await listBatches();
      } finally {
        this.loading = false;
      }
    },
    setWarehouse(id: number) {
      this.warehouseFilter = id;
    }
  }
});
