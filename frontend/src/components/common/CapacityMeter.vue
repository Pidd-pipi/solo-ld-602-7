<script setup lang="ts">
import { computed } from "vue";
import { formatNumber } from "../../utils/formatters";

const props = defineProps<{
  capacity: number;
  population: number;
  status?: string;
}>();

const percent = computed(() => {
  if (!props.capacity || props.capacity <= 0) return 0;
  return Math.min(100, Math.round((props.population / props.capacity) * 100));
});

const type = computed(() => {
  if (percent.value >= 100) return "exception";
  if (percent.value >= 80) return "warning";
  return "success";
});
</script>

<template>
  <div class="meter">
    <el-progress :percentage="percent" :status="type === 'exception' ? 'exception' : undefined" :stroke-width="10" />
    <div class="legend">
      <span>{{ formatNumber(population) }} / {{ formatNumber(capacity) }} 人</span>
      <el-tag v-if="status" size="small">{{ status }}</el-tag>
    </div>
  </div>
</template>

<style scoped>
.meter { min-width: 140px; }
.legend { display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: var(--el-text-color-secondary); margin-top: 2px; }
</style>
