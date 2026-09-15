import { defineStore } from "pinia";
import { listInventoryBatch } from "../api/InventoryBatch";
export const useInventoryBatchStore = defineStore("inventoryBatch", {
  state: () => ({ rows: [] as Awaited<ReturnType<typeof listInventoryBatch>>, loading: false }),
  actions: { async load() { this.loading = true; this.rows = await listInventoryBatch(); this.loading = false; } }
});
