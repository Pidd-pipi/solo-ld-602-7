import { defineStore } from "pinia";
import { listDispatchOrder } from "../api/DispatchOrder";
export const useDispatchOrderStore = defineStore("dispatchOrder", {
  state: () => ({ rows: [] as Awaited<ReturnType<typeof listDispatchOrder>>, loading: false }),
  actions: { async load() { this.loading = true; this.rows = await listDispatchOrder(); this.loading = false; } }
});
