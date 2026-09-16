<script setup lang="ts">
import type { NearExpireRow } from "../../types/Dashboard";
import { daysLeftText, formatNumber } from "../../utils/formatters";

defineProps<{ rows: NearExpireRow[] }>();
</script>

<template>
  <div class="warning-list">
    <el-empty v-if="!rows.length" description="暂无临期物资" :image-size="60" />
    <div v-for="r in rows" :key="r.batch_no + r.warehouse_name" class="item">
      <div class="main">
        <strong>{{ r.supply_name }}</strong>
        <span class="muted">{{ r.batch_no }} · {{ r.warehouse_name }}</span>
      </div>
      <div class="side">
        <el-tag :type="r.days_left < 0 ? 'danger' : 'warning'" size="small">{{ daysLeftText(r.days_left) }}</el-tag>
        <span class="qty">在库 {{ formatNumber(r.quantity) }}{{ r.unit }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.warning-list { display: flex; flex-direction: column; gap: 8px; }
.item { display: flex; justify-content: space-between; align-items: center; padding: 6px 8px; background: var(--el-fill-color-light); border-radius: 6px; }
.main { display: flex; flex-direction: column; gap: 2px; }
.muted, .qty { color: var(--el-text-color-secondary); font-size: 12px; }
.side { display: flex; align-items: center; gap: 8px; }
</style>
