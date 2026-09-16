import { defineStore } from "pinia";
import { listShelters, updateShelter, listReceiveRecords } from "../api/Shelter";
import type { Shelter } from "../types/Shelter";
import type { ReceiveRecord } from "../api/Shelter";

export const useShelterStore = defineStore("shelter", {
  state: () => ({
    rows: [] as Shelter[],
    loading: false,
    receiveRecords: {} as Record<number, ReceiveRecord[]>
  }),
  actions: {
    async load() {
      this.loading = true;
      try {
        this.rows = await listShelters();
      } finally {
        this.loading = false;
      }
    },
    async save(id: number, body: Partial<Shelter>) {
      await updateShelter(id, body);
      await this.load();
    },
    async loadReceiveRecords(shelterId: number) {
      this.receiveRecords[shelterId] = await listReceiveRecords(shelterId);
    }
  }
});
