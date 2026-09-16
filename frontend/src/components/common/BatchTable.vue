<script setup lang="ts">
import { computed } from "vue";
import type { InventoryBatch } from "../../types/InventoryBatch";
import { availableOf } from "../../types/InventoryBatch";
import { SupplyCategoryText } from "../../constants/SupplyCategory";
import { daysLeftText, formatDate, formatNumber } from "../../utils/formatters";

const props = defineProps<{
  rows: InventoryBatch[];
  showAvailable?: boolean;
  highlightBatchIds?: number[];
  compact?: boolean;
}>();

const daysLeft = (v: string | null): number => {
  if (!v) return Number.POSITIVE_INFINITY;
  return Math.ceil((new Date(v.replace(" ", "T")).getTime() - Date.now()) / 86400000);
};

const enriched = computed(() =>
  props.rows.map((b) => ({ ...b, available: availableOf(b), days: daysLeft(b.expire_at) }))
);
</script>

<template>
  <el-table :data="enriched" size="small" stripe :max-height="compact ? 320 : undefined">
    <el-table-column label="批次号" prop="batch_no" min-width="150" />
    <el-table-column label="物资" min-width="160">
      <template #default="{ row }">
        <div>{{ row.supply_name ?? `物资#${row.supply_item_id}` }}</div>
        <div class="sub">{{ row.sku_code }} · {{ SupplyCategoryText[row.category as keyof typeof SupplyCategoryText] ?? row.category }}</div>
      </template>
    </el-table-column>
    <el-table-column v-if="!compact" label="所属仓库" prop="warehouse_name" min-width="150" />
    <el-table-column label="在库量" width="90" align="right">
      <template #default="{ row }">{{ formatNumber(row.quantity) }}</template>
    </el-table-column>
    <el-table-column label="已占用" width="90" align="right">
      <template #default="{ row }">
        <span :class="{ held: row.held_quantity > 0 }">{{ formatNumber(row.held_quantity) }}</span>
      </template>
    </el-table-column>
    <el-table-column v-if="showAvailable" label="可用" width="90" align="right">
      <template #default="{ row }">
        <strong :class="{ low: row.available <= 0 }">{{ formatNumber(row.available) }}</strong>
      </template>
    </el-table-column>
    <el-table-column label="到期" min-width="150">
      <template #default="{ row }">
        <template v-if="row.expire_at">
          <el-tag v-if="row.days < 0" type="danger" size="small">已过期</el-tag>
          <el-tag v-else-if="row.days <= 30" type="warning" size="small">{{ daysLeftText(row.days) }}</el-tag>
          <span v-else>{{ formatDate(row.expire_at) }}</span>
        </template>
        <span v-else>—</span>
      </template>
    </el-table-column>
    <el-table-column v-if="!compact" label="质量" width="90">
      <template #default="{ row }">
        <el-tag size="small" :type="row.quality_status === 'OK' ? 'success' : row.quality_status === 'DAMAGED' ? 'danger' : 'warning'">
          {{ row.quality_status === "OK" ? "正常" : row.quality_status === "DAMAGED" ? "破损" : "临期" }}
        </el-tag>
      </template>
    </el-table-column>
  </el-table>
</template>

<style scoped>
.sub { color: var(--el-text-color-secondary); font-size: 12px; }
.held { color: var(--el-color-warning); }
.low { color: var(--el-color-danger); }
</style>
