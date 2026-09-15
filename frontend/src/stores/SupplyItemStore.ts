import { defineStore } from "pinia";
import { listSupplyItem } from "../api/SupplyItem";
export const useSupplyItemStore = defineStore("supplyItem", {
  state: () => ({ rows: [] as Awaited<ReturnType<typeof listSupplyItem>>, loading: false }),
  actions: { async load() { this.loading = true; this.rows = await listSupplyItem(); this.loading = false; } }
});
