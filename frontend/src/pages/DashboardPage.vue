<script setup lang="ts">
import { computed, onMounted } from "vue";
import { storeToRefs } from "pinia";
import { useDashboardStore } from "../stores/DashboardStore";
import StatCard from "../components/common/StatCard.vue";
import ExpireWarningList from "../components/common/ExpireWarningList.vue";
import StatusBadge from "../components/common/StatusBadge.vue";
import { DispatchStatusText } from "../constants/DispatchStatus";
import { LedgerDirectionText } from "../constants/LedgerDirection";
import { formatDate, formatNumber } from "../utils/formatters";

const dashboard = useDashboardStore();
const { overview, loadedAt } = storeToRefs(dashboard);

onMounted(() => dashboard.load());

const matrix = computed(() =>
  overview.value.statusMatrix.map((m) => ({
    status: m.status,
    count: m.count,
    text: DispatchStatusText[m.status as keyof typeof DispatchStatusText] ?? m.status
  }))
);
</script>

<template>
  <div v-loading="dashboard.loading">
    <div class="page-toolbar">
      <span class="muted small">数据实时聚合自库存批次与调拨状态</span>
      <div class="spacer" />
      <span class="muted small" v-if="loadedAt">更新于 {{ loadedAt }}</span>
      <el-button size="small" @click="dashboard.load()">刷新态势</el-button>
    </div>

    <div class="grid grid-4">
      <StatCard label="在库物资总量" :value="formatNumber(overview.stock.total_qty)" unit="件" tone="primary"
        :hint="`${overview.stock.warehouse_count} 个仓库 · ${overview.stock.batch_count} 个批次`" />
      <StatCard label="已占用（待出库）" :value="formatNumber(overview.stock.held_qty)" unit="件" tone="warning"
        hint="申请占用，审批不扣减" />
      <StatCard label="当前可用库存" :value="formatNumber(overview.stock.available_qty)" unit="件" tone="success"
        hint="可用 = 在库 − 已占用" />
      <StatCard label="临期批次" :value="overview.nearExpireBatches" :unit="`批 / ${overview.nearExpireDays}天`" tone="danger" />
    </div>

    <div class="grid grid-4" style="margin-top:14px">
      <StatCard label="待审批调拨" :value="overview.orders.pending_approval" tone="warning" />
      <StatCard label="已批待出库" :value="overview.orders.approved_waiting_outbound" tone="primary" />
      <StatCard label="出库在途" :value="overview.orders.in_transit" tone="primary" />
      <StatCard label="已签收" :value="overview.orders.received" tone="success" />
    </div>

    <div class="grid grid-3" style="margin-top:14px">
      <div class="panel" style="grid-column: span 2">
        <h3>各物资库存与占用</h3>
        <el-table :data="overview.byItem" size="small">
          <el-table-column label="物资" prop="name" min-width="160" />
          <el-table-column label="分类" width="80">
            <template #default="{ row }">{{ row.category }}</template>
          </el-table-column>
          <el-table-column label="在库" width="90" align="right">
            <template #default="{ row }">{{ formatNumber(row.total_qty) }}</template>
          </el-table-column>
          <el-table-column label="占用" width="90" align="right">
            <template #default="{ row }">
              <span :style="{ color: row.held_qty > 0 ? '#b45309' : '' }">{{ formatNumber(row.held_qty) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="可用" min-width="160">
            <template #default="{ row }">
              <div class="small muted">{{ formatNumber(row.available_qty) }} / 安全库存 {{ formatNumber(row.safety_stock) }}</div>
              <div class="stock-bar">
                <div class="held" :style="{ width: (row.total_qty ? (row.held_qty / row.total_qty) * 100 : 0) + '%' }" />
                <div class="avail" :style="{ width: (row.total_qty ? (row.available_qty / row.total_qty) * 100 : 0) + '%' }" />
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="panel">
        <h3>调拨状态分布</h3>
        <div class="grid grid-2">
          <div v-for="m in matrix" :key="m.status" class="status-cell">
            <StatusBadge :value="m.status" />
            <strong style="margin-left:8px">{{ m.count }}</strong>
          </div>
        </div>
        <el-divider />
        <div class="small muted">异常闭环：已驳回 / 拒签 / 撤销 共 {{ overview.orders.closed_abnormal }} 单，库存均已按原批次回补。</div>
      </div>
    </div>

    <div class="grid grid-2" style="margin-top:14px">
      <div class="panel">
        <h3>临期物资预警</h3>
        <ExpireWarningList :rows="overview.nearExpireList" />
      </div>
      <div class="panel">
        <h3>最新库存流水</h3>
        <el-table :data="overview.recentLedger" size="small" max-height="320">
          <el-table-column label="时间" width="150">
            <template #default="{ row }">{{ formatDate(row.created_at) }}</template>
          </el-table-column>
          <el-table-column label="方向" width="92">
            <template #default="{ row }">
              <el-tag size="small" :type="row.direction === 'OUTBOUND' ? 'primary' : row.direction === 'HOLD' ? 'warning' : 'success'">
                {{ LedgerDirectionText[row.direction as keyof typeof LedgerDirectionText] ?? row.direction }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="物资/批次" min-width="160">
            <template #default="{ row }">
              <div>{{ row.supply_name }}</div>
              <div class="small muted">{{ row.batch_no }}</div>
            </template>
          </el-table-column>
          <el-table-column label="数量" width="70" align="right">
            <template #default="{ row }">{{ formatNumber(row.change_qty) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.status-cell { display: flex; align-items: center; padding: 6px 4px; border-bottom: 1px dashed #eef0f3; }
</style>
