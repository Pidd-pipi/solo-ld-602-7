import { defineStore } from "pinia";
import { listWarehouse } from "../api/Warehouse";
export const useWarehouseStore = defineStore("warehouse", {
  state: () => ({ rows: [] as Awaited<ReturnType<typeof listWarehouse>>, loading: false }),
  actions: { async load() { this.loading = true; this.rows = await listWarehouse(); this.loading = false; } }
});
