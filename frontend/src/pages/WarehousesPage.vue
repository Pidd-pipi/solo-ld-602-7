<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { storeToRefs } from "pinia";
import { useInventoryBatchStore } from "../stores/InventoryBatchStore";
import { useCatalogStore } from "../stores/CatalogStore";
import BatchTable from "../components/common/BatchTable.vue";
import StatCard from "../components/common/StatCard.vue";
import { useExpireWarning } from "../hooks/useExpireWarning";
import { availableOf } from "../types/InventoryBatch";
import { formatNumber } from "../utils/formatters";

const batchStore = useInventoryBatchStore();
const catalog = useCatalogStore();
const { rows, loading } = storeToRefs(batchStore);

const warehouseId = ref<number>(0);
const onlyNearExpire = ref(false);

onMounted(async () => {
  await catalog.load();
  await batchStore.load();
});

const scoped = computed(() =>
  warehouseId.value ? rows.value.filter((b) => b.warehouse_id === warehouseId.value) : rows.value
);
const { enriched: expireRows } = useExpireWarning(scoped, 30);
const expireBatchIds = computed(() => new Set(expireRows.value.map((b) => b.id)));

const visible = computed(() =>
  onlyNearExpire.value ? scoped.value.filter((b) => expireBatchIds.value.has(b.id)) : scoped.value
);

const totals = computed(() => {
  const list = visible.value;
  return {
    qty: list.reduce((s, b) => s + b.quantity, 0),
    held: list.reduce((s, b) => s + b.held_quantity, 0),
    avail: list.reduce((s, b) => s + availableOf(b), 0)
  };
});
</script>

<template>
  <div v-loading="loading">
    <div class="page-toolbar">
      <el-select v-model="warehouseId" placeholder="全部仓库" style="width: 220px">
        <el-option :value="0" label="全部仓库" />
        <el-option v-for="w in catalog.warehouses" :key="w.id" :value="w.id" :label="w.name" />
      </el-select>
      <el-radio-group v-model="onlyNearExpire">
        <el-radio-button :value="false">全部批次</el-radio-button>
        <el-radio-button :value="true">仅临期（30天）</el-radio-button>
      </el-radio-group>
      <div class="spacer" />
      <el-button size="small" @click="batchStore.load()">刷新库存</el-button>
    </div>

    <div class="grid grid-3" style="margin-bottom:14px">
      <StatCard label="在库总量" :value="formatNumber(totals.qty)" unit="件" tone="primary" />
      <StatCard label="已被调拨占用" :value="formatNumber(totals.held)" unit="件" tone="warning" />
      <StatCard label="可用库存" :value="formatNumber(totals.avail)" unit="件" tone="success" />
    </div>

    <div class="panel">
      <h3>库存批次（申请只占用“可用”列，出库时按批次扣减）</h3>
      <BatchTable :rows="visible" show-available />
    </div>
  </div>
</template>
