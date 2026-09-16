<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { storeToRefs } from "pinia";
import { useDispatchStore } from "../stores/DispatchOrderStore";
import { useCatalogStore } from "../stores/CatalogStore";
import { useInventoryBatchStore } from "../stores/InventoryBatchStore";
import { useAuthStore } from "../stores/auth";
import { getDispatchDetail } from "../api/DispatchOrder";
import { useDispatchFlow } from "../hooks/useDispatchFlow";
import { usePagination } from "../hooks/usePagination";
import { availableOf } from "../types/InventoryBatch";
import { createDispatchApplyForm, createApplyLine } from "../constructors/DispatchOrderConstructor";
import type { ApplyDispatchPayload } from "../types/api";
import type { DispatchOrder, DispatchDetail } from "../types/DispatchOrder";
import type { DispatchActionName } from "../constants/permission";
import { canDispatch } from "../constants/permission";
import {
  DispatchStatus,
  DispatchStatusText
} from "../constants/DispatchStatus";
import { AllocationStageText } from "../constants/LedgerDirection";
import { PRIORITY_TEXT } from "../constants/statusText";
import StatusBadge from "../components/common/StatusBadge.vue";
import ApprovalTimeline from "../components/common/ApprovalTimeline.vue";
import EmptyState from "../components/common/EmptyState.vue";
import { formatDate, formatNumber } from "../utils/formatters";

const store = useDispatchStore();
const catalog = useCatalogStore();
const batchStore = useInventoryBatchStore();
const auth = useAuthStore();
const { rows, countByStatus, statusFilter, loading } = storeToRefs(store);
const flow = useDispatchFlow();

const search = ref("");

const visible = computed(() =>
  store.filtered.filter((o) => {
    if (!search.value.trim()) return true;
    const q = search.value.trim();
    return (
      String(o.id).includes(q) ||
      (o.warehouse_name ?? "").includes(q) ||
      (o.shelter_name ?? "").includes(q) ||
      (o.event_title ?? "").includes(q)
    );
  })
);
const { page, pageRows, totalPages, prev, next } = usePagination(visible, 8);

const filterOptions = computed(() => [
  { value: "", label: "全部", count: rows.value.length },
  ...DispatchStatus.map((s) => ({ value: s, label: DispatchStatusText[s], count: countByStatus.value[s] ?? 0 }))
]);

// ---- 申请弹窗 ----
const applyVisible = ref(false);
const form = ref<ApplyDispatchPayload>(createDispatchApplyForm());

const openApply = () => {
  form.value = createDispatchApplyForm({
    eventId: catalog.events[0]?.id ?? null,
    sourceWarehouseId: catalog.activeWarehouses[0]?.id ?? 0,
    shelterId: catalog.shelters[0]?.id ?? 0
  });
  if (!form.value.lines.length) form.value.lines.push(createApplyLine(catalog.supplyItems[0]?.id ?? 0, 1));
  applyVisible.value = true;
};

const addLine = () => form.value.lines.push(createApplyLine(catalog.supplyItems[0]?.id ?? 0, 1));
const removeLine = (i: number) => form.value.lines.splice(i, 1);

/** 当前所选仓库下各物资的可用量（实时来自批次 quantity - held）。 */
const availableFor = (itemId: number): number =>
  batchStore.rows
    .filter((b) => b.warehouse_id === form.value.sourceWarehouseId && b.supply_item_id === itemId)
    .reduce((s, b) => s + availableOf(b), 0);

const totalRequested = computed(() => form.value.lines.reduce((s, l) => s + (l.requestedQty || 0), 0));

const submitApply = async () => {
  if (!form.value.shelterId || !form.value.sourceWarehouseId) return;
  const valid = form.value.lines.every((l) => l.supplyItemId && l.requestedQty > 0);
  if (!valid) return;
  const result = await flow.submit(form.value);
  if (result.ok) {
    applyVisible.value = false;
    page.value = 1;
  }
};

/** 重复提交演示：沿用同一 requestId 再发一次，后端幂等返回，不重复占库。 */
const resubmitSameRequest = async () => {
  const payload = createDispatchApplyForm({ ...form.value });
  const result = await flow.submit(payload);
  if (result.ok) applyVisible.value = false;
};

// ---- 审批详情抽屉 ----
const detailVisible = ref(false);
const detail = ref<DispatchDetail | null>(null);
const detailLoading = ref(false);

const openDetail = async (order: DispatchOrder) => {
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    detail.value = await getDispatchDetail(order.id);
  } finally {
    detailLoading.value = false;
  }
};

const doAction = async (order: DispatchOrder, key: DispatchActionName) => {
  const ok = await flow.runAction(order, key);
  if (ok && detail.value && detail.value.id === order.id) {
    detail.value = await getDispatchDetail(order.id);
  }
};

onMounted(async () => {
  await catalog.load();
  await Promise.all([store.load(), batchStore.load()]);
});
</script>

<template>
  <div v-loading="loading">
    <div class="page-toolbar">
      <el-input v-model="search" placeholder="搜索单号 / 仓库 / 避难点 / 事件" clearable style="width: 260px" />
      <el-button v-if="canDispatch(auth.role, 'apply')" type="primary" size="small" @click="openApply">新建调拨申请</el-button>
    </div>

    <div class="filter-chips" style="margin-bottom: 14px">
      <button
        v-for="f in filterOptions"
        :key="f.value || 'all'"
        :class="{ active: statusFilter === f.value }"
        @click="store.setFilter(f.value)"
      >
        {{ f.label }} {{ f.count }}
      </button>
    </div>

    <div class="panel">
      <el-table :data="pageRows" size="small" stripe @row-click="openDetail">
        <el-table-column label="单号" width="70">
          <template #default="{ row }">#{{ row.id }}</template>
        </el-table-column>
        <el-table-column label="事件" min-width="140">
          <template #default="{ row }">{{ row.event_title ?? "—" }}</template>
        </el-table-column>
        <el-table-column label="调拨方向" min-width="220">
          <template #default="{ row }">
            <div>{{ row.warehouse_name }} → {{ row.shelter_name }}</div>
            <div class="muted small">{{ row.shelter_district }} · {{ PRIORITY_TEXT[row.priority] ?? row.priority }}</div>
          </template>
        </el-table-column>
        <el-table-column label="物资" width="90" align="right">
          <template #default="{ row }">{{ row.line_count ?? 0 }} 种</template>
        </el-table-column>
        <el-table-column label="申请/出库" width="110" align="right">
          <template #default="{ row }">
            {{ formatNumber(row.total_requested) }}
            <span class="muted">/ {{ formatNumber(row.total_outbound) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusBadge :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ formatDate(row.submitted_at) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link size="small" @click.stop="openDetail(row)">详情</el-button>
            <el-button
              v-for="a in flow.availableActions(row)"
              :key="a"
              link
              size="small"
              :type="flow.actionDef(a).danger ? 'danger' : 'primary'"
              @click.stop="doAction(row, a)"
            >
              {{ flow.actionDef(a).label }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty><EmptyState description="暂无调拨单" /></template>
      </el-table>

      <div style="display:flex;justify-content:flex-end;align-items:center;gap:10px;margin-top:12px">
        <span class="muted small">{{ page }} / {{ totalPages }}</span>
        <el-button size="small" :disabled="page <= 1" @click="prev">上一页</el-button>
        <el-button size="small" :disabled="page >= totalPages" @click="next">下一页</el-button>
      </div>
    </div>

    <!-- 新建申请 -->
    <el-dialog v-model="applyVisible" title="新建调拨申请（提交即占用可用库存）" width="640px">
      <el-form :model="form" label-width="92px">
        <el-form-item label="灾害事件">
          <el-select v-model="form.eventId" style="width: 100%">
            <el-option v-for="e in catalog.events" :key="e.id" :value="e.id" :label="e.title" />
          </el-select>
        </el-form-item>
        <el-form-item label="源仓库">
          <el-select v-model="form.sourceWarehouseId" style="width: 100%">
            <el-option v-for="w in catalog.activeWarehouses" :key="w.id" :value="w.id" :label="`${w.name}（${w.district}）`" />
          </el-select>
        </el-form-item>
        <el-form-item label="避难点">
          <el-select v-model="form.shelterId" style="width: 100%">
            <el-option v-for="s in catalog.shelters" :key="s.id" :value="s.id" :label="`${s.name}（${s.district}）`" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority">
            <el-option label="常规" value="NORMAL" />
            <el-option label="紧急" value="HIGH" />
            <el-option label="特急" value="CRITICAL" />
          </el-select>
        </el-form-item>

        <el-divider content-position="left">物资明细（提交后按批次 FEFO 占用）</el-divider>
        <div v-for="(line, i) in form.lines" :key="i" class="line-row">
          <el-select v-model="line.supplyItemId" style="flex:1">
            <el-option v-for="item in catalog.supplyItems" :key="item.id" :value="item.id" :label="`${item.name}（${item.unit}）`" />
          </el-select>
          <el-input-number v-model="line.requestedQty" :min="1" />
          <el-tag :type="availableFor(line.supplyItemId) >= line.requestedQty ? 'success' : 'danger'" size="small">
            可用 {{ availableFor(line.supplyItemId) }}
          </el-tag>
          <el-button link type="danger" :disabled="form.lines.length <= 1" @click="removeLine(i)">删除</el-button>
        </div>
        <el-button size="small" plain style="margin-top:8px" @click="addLine">+ 添加物资</el-button>

        <el-form-item label="备注" style="margin-top:12px">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
        <div class="muted small">申请单号 requestId：{{ form.requestId }}（重复提交同一申请不会二次占库）· 合计 {{ totalRequested }} 件</div>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="warning" plain @click="resubmitSameRequest">模拟重复提交</el-button>
        <el-button type="primary" @click="submitApply">提交申请并占库</el-button>
      </template>
    </el-dialog>

    <!-- 审批详情 -->
    <el-drawer v-model="detailVisible" title="调拨审批详情" size="600px">
      <div v-loading="detailLoading" class="detail-drawer" v-if="detail">
        <div style="display:flex;align-items:center;gap:10px">
          <h3 style="margin:0">调拨单 #{{ detail.id }}</h3>
          <StatusBadge :value="detail.status" />
        </div>

        <div class="kv" style="margin-top:12px">
          <span class="k">灾害事件</span><span>{{ detail.event_title ?? "—" }}</span>
          <span class="k">调拨方向</span><span>{{ detail.warehouse_name }} → {{ detail.shelter_name }}</span>
          <span class="k">申请人</span><span>{{ detail.requester_name ?? "—" }}</span>
          <span class="k">优先级</span><span>{{ PRIORITY_TEXT[detail.priority] ?? detail.priority }}</span>
          <span class="k">提交时间</span><span>{{ formatDate(detail.submitted_at) }}</span>
          <span v-if="detail.approved_at"><span class="k">审批时间</span><span>{{ formatDate(detail.approved_at) }}</span></span>
          <span v-if="detail.dispatched_at"><span class="k">出库时间</span><span>{{ formatDate(detail.dispatched_at) }}</span></span>
          <span v-if="detail.received_at"><span class="k">签收时间</span><span>{{ formatDate(detail.received_at) }}</span></span>
          <span v-if="detail.reject_reason"><span class="k">驳回原因</span><span>{{ detail.reject_reason }}</span></span>
          <span v-if="detail.refuse_reason"><span class="k">拒签原因</span><span>{{ detail.refuse_reason }}</span></span>
          <span v-if="detail.cancel_reason"><span class="k">撤销原因</span><span>{{ detail.cancel_reason }}</span></span>
        </div>

        <div style="display:flex;gap:8px;margin:16px 0" v-if="flow.availableActions(detail).length">
          <el-button
            v-for="a in flow.availableActions(detail)"
            :key="a"
            :type="flow.actionDef(a).danger ? 'danger' : 'primary'"
            @click="doAction(detail, a)"
          >
            {{ flow.actionDef(a).label }}
          </el-button>
        </div>

        <div class="section-title">申请明细</div>
        <el-table :data="detail.lines" size="small" border>
          <el-table-column label="物资" min-width="180">
            <template #default="{ row }">{{ row.supply_name }}（{{ row.unit }}）</template>
          </el-table-column>
          <el-table-column label="申请数量" prop="requested_qty" width="100" align="right" />
          <el-table-column label="已出库" prop="outbound_qty" width="90" align="right" />
        </el-table>

        <div class="section-title">批次去向（占用 / 出库 / 回补）</div>
        <el-table :data="detail.allocations" size="small" border>
          <el-table-column label="批次号" prop="batch_no" min-width="150" />
          <el-table-column label="物资" min-width="130">
            <template #default="{ row }">{{ row.supply_name }}</template>
          </el-table-column>
          <el-table-column label="占用/出库量" prop="allocated_qty" width="100" align="right" />
          <el-table-column label="已回补" prop="returned_qty" width="80" align="right" />
          <el-table-column label="阶段" width="92">
            <template #default="{ row }">
              <el-tag size="small" :type="row.stage === 'OUT' ? 'primary' : row.stage === 'RETURNED' ? 'info' : 'warning'">
                {{ AllocationStageText[row.stage as keyof typeof AllocationStageText] }}
              </el-tag>
            </template>
          </el-table-column>
          <template #empty><EmptyState description="暂无批次去向" :image-size="48" /></template>
        </el-table>

        <div class="section-title">流转时间线</div>
        <ApprovalTimeline :entries="detail.timeline" />
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.line-row { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
</style>
