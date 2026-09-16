<script setup lang="ts">
import { computed } from "vue";
import type { TimelineEntry } from "../../types/DispatchOrder";
import { formatDate } from "../../utils/formatters";

const props = defineProps<{ entries: TimelineEntry[] }>();

const ACTION_META: Record<string, { text: string; type: "primary" | "success" | "warning" | "danger" | "info" }> = {
  SUBMIT: { text: "提交申请 · 占用库存", type: "primary" },
  APPROVE: { text: "审批通过（不扣减库存）", type: "success" },
  REJECT: { text: "审批驳回 · 释放占用", type: "danger" },
  OUTBOUND: { text: "出库 · 按批次扣减", type: "warning" },
  RECEIVE: { text: "签收确认", type: "success" },
  REFUSE: { text: "拒签 · 原批次回补", type: "danger" },
  CANCEL: { text: "撤销 · 释放占用 / 原批次回补", type: "info" }
};

const nodes = computed(() =>
  [...props.entries]
    .sort((a, b) => a.id - b.id)
    .map((e) => ({
      key: e.id,
      meta: ACTION_META[e.action] ?? { text: e.action, type: "info" as const },
      actor: e.actor || "系统",
      note: e.note,
      time: formatDate(e.created_at)
    }))
);
</script>

<template>
  <el-timeline v-if="nodes.length">
    <el-timeline-item
      v-for="n in nodes"
      :key="n.key"
      :type="n.meta.type"
      :timestamp="`${n.time} · ${n.actor}`"
      placement="top"
    >
      <strong>{{ n.meta.text }}</strong>
      <div v-if="n.note" class="note">原因：{{ n.note }}</div>
    </el-timeline-item>
  </el-timeline>
  <el-empty v-else description="暂无流转记录" :image-size="60" />
</template>

<style scoped>
.note { color: var(--el-text-color-secondary); font-size: 12px; margin-top: 2px; }
</style>
