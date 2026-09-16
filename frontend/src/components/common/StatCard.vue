<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  label: string;
  value: number | string;
  unit?: string;
  tone?: "default" | "warning" | "danger" | "success" | "primary";
  hint?: string;
}>();

const cls = computed(() => `stat-card tone-${props.tone ?? "default"}`);
</script>

<template>
  <div :class="cls">
    <div class="label">{{ label }}</div>
    <div class="value">{{ value }}<span v-if="unit" class="unit">{{ unit }}</span></div>
    <div v-if="hint" class="hint">{{ hint }}</div>
  </div>
</template>

<style scoped>
.stat-card { background: var(--el-bg-color); border: 1px solid var(--el-border-color-lighter); border-radius: 10px; padding: 14px 16px; min-width: 140px; }
.label { color: var(--el-text-color-secondary); font-size: 13px; }
.value { font-size: 26px; font-weight: 700; margin-top: 4px; }
.unit { font-size: 13px; font-weight: 400; margin-left: 4px; color: var(--el-text-color-secondary); }
.hint { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 4px; }
.tone-warning .value { color: var(--el-color-warning); }
.tone-danger .value { color: var(--el-color-danger); }
.tone-success .value { color: var(--el-color-success); }
.tone-primary .value { color: var(--el-color-primary); }
</style>
