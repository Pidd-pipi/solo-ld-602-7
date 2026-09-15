import { defineStore } from "pinia";
import { listShelter } from "../api/Shelter";
export const useShelterStore = defineStore("shelter", {
  state: () => ({ rows: [] as Awaited<ReturnType<typeof listShelter>>, loading: false }),
  actions: { async load() { this.loading = true; this.rows = await listShelter(); this.loading = false; } }
});
