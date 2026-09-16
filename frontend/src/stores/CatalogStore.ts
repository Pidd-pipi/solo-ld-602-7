import { defineStore } from "pinia";
import { listWarehouses } from "../api/Warehouse";
import { listSupplyItems } from "../api/SupplyItem";
import { listEvents } from "../api/Event";
import { listShelters } from "../api/Shelter";
import type { Warehouse } from "../types/Warehouse";
import type { SupplyItem } from "../types/SupplyItem";
import type { Shelter } from "../types/Shelter";
import type { DisasterEvent } from "../types/DisasterEvent";

/** 申请表单所需的基础档案：仓库/物资/避难点/事件。 */
export const useCatalogStore = defineStore("catalog", {
  state: () => ({
    warehouses: [] as Warehouse[],
    supplyItems: [] as SupplyItem[],
    shelters: [] as Shelter[],
    events: [] as DisasterEvent[],
    loaded: false
  }),
  getters: {
    activeWarehouses: (s) => s.warehouses.filter((w) => w.status === "ACTIVE")
  },
  actions: {
    async load(force = false) {
      if (this.loaded && !force) return;
      const [warehouses, supplyItems, shelters, events] = await Promise.all([
        listWarehouses(),
        listSupplyItems(),
        listShelters(),
        listEvents()
      ]);
      this.warehouses = warehouses;
      this.supplyItems = supplyItems;
      this.shelters = shelters;
      this.events = events;
      this.loaded = true;
    },
    itemName(id: number): string {
      return this.supplyItems.find((i) => i.id === id)?.name ?? `物资#${id}`;
    },
    warehouseName(id: number): string {
      return this.warehouses.find((w) => w.id === id)?.name ?? `仓库#${id}`;
    },
    shelterName(id: number): string {
      return this.shelters.find((s) => s.id === id)?.name ?? `避难点#${id}`;
    }
  }
});
