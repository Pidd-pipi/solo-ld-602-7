<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { storeToRefs } from "pinia";
import { ElMessage } from "element-plus";
import { useShelterStore } from "../stores/ShelterStore";
import { useAuthStore } from "../stores/auth";
import { useCatalogStore } from "../stores/CatalogStore";
import type { Shelter } from "../types/Shelter";
import type { ReceiveRecord } from "../api/Shelter";
import StatusBadge from "../components/common/StatusBadge.vue";
import CapacityMeter from "../components/common/CapacityMeter.vue";
import EmptyState from "../components/common/EmptyState.vue";
import { RISK_TEXT } from "../constants/statusText";
import { ShelterStatus, ShelterStatusText } from "../constants/ShelterStatus";
import { RequestError } from "../utils/request";

const store = useShelterStore();
const catalog = useCatalogStore();
const auth = useAuthStore();
const { rows, loading, receiveRecords } = storeToRefs(store);

const editing = ref<Shelter | null>(null);
const formRef = ref();
const form = ref<Partial<Shelter>>({});
const drawerShelter = ref<Shelter | null>(null);
const drawerVisible = computed({
  get: () => drawerShelter.value !== null,
  set: (v) => {
    if (!v) drawerShelter.value = null;
  }
});
const drawerTitle = computed(() => `接收记录 · ${drawerShelter.value?.name ?? ""}`);
const editVisible = computed({
  get: () => editing.value !== null,
  set: (v) => {
    if (!v) editing.value = null;
  }
});

onMounted(async () => {
  await catalog.load();
  await store.load();
});

const openEdit = (s: Shelter) => {
  editing.value = s;
  form.value = { ...s };
};

const save = async () => {
  if (!editing.value) return;
  try {
    await store.save(editing.value.id, form.value);
    ElMessage.success("避难点已更新");
    editing.value = null;
  }  catch (e) {
    ElMessage.error(e instanceof RequestError ? e.message : "保存失败");
  }
};

const openRecords = async (s: Shelter) => {
  drawerShelter.value = s;
  await store.loadReceiveRecords(s.id);
};

const recordTag = (status: string) =>
  ({ RECEIVED: "success", DISPATCHED: "primary", REJECTED: "danger", REFUSED: "danger", CANCELLED: "info", APPROVED: "warning", SUBMITTED: "warning" } as Record<string, string>)[status] ?? "info";
</script>

<template>
  <div v-loading="loading">
    <div class="page-toolbar">
      <span class="muted small">开放状态与容量由街道管理员维护；接收记录随调拨状态实时变化</span>
    </div>

    <div class="grid grid-2">
      <div v-for="s in rows" :key="s.id" class="panel">
        <div style="display:flex;justify-content:space-between;align-items:flex-start">
          <div>
            <h3 style="margin-bottom:4px">{{ s.name }}</h3>
            <div class="muted small">{{ s.district }} · 风险等级 {{ RISK_TEXT[s.risk_level] ?? s.risk_level }} · {{ s.contact_person }} {{ s.contact_phone }}</div>
          </div>
          <StatusBadge :value="s.open_status" domain="shelter" />
        </div>
        <div style="margin:12px 0">
          <CapacityMeter :capacity="s.capacity" :population="s.current_population" :status="ShelterStatusText[s.open_status]" />
        </div>
        <div class="small muted" style="margin-bottom:10px">
          在途调拨 {{ s.inbound_in_transit ?? 0 }} 单 · 已签收 {{ s.received_orders ?? 0 }} 单
        </div>
        <div style="display:flex;gap:8px">
          <el-button size="small" @click="openRecords(s)">接收记录</el-button>
          <el-button v-if="auth.role === 'STREET_ADMIN'" size="small" type="primary" plain @click="openEdit(s)">维护</el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="editVisible" title="维护避难点" width="460px">
      <el-form :model="form" label-width="92px">
        <el-form-item label="容量"><el-input-number v-model="form.capacity" :min="0" /></el-form-item>
        <el-form-item label="当前人数"><el-input-number v-model="form.current_population" :min="0" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contact_person" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.contact_phone" /></el-form-item>
        <el-form-item label="风险等级">
          <el-select v-model="form.risk_level">
            <el-option v-for="(text, key) in RISK_TEXT" :key="key" :value="key" :label="text" />
          </el-select>
        </el-form-item>
        <el-form-item label="开放状态">
          <el-select v-model="form.open_status">
            <el-option v-for="st in ShelterStatus" :key="st" :value="st" :label="ShelterStatusText[st]" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editing = null">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="460px">
      <template v-if="drawerShelter">
        <div v-if="!(receiveRecords[drawerShelter.id]?.length)" class="panel"><EmptyState description="暂无调拨记录" /></div>
        <el-timeline v-else>
          <el-timeline-item v-for="r in receiveRecords[drawerShelter.id] as ReceiveRecord[]" :key="r.id" :type="recordTag(r.status) as any">
            <div style="display:flex;align-items:center;gap:8px">
              <strong>调拨单 #{{ r.id }}</strong>
              <StatusBadge :value="r.status" />
              <span class="muted small">{{ r.total_requested }} 件</span>
            </div>
            <div class="muted small" style="margin-top:2px">
              来源：{{ r.warehouse_name }} · 出库 {{ r.dispatched_at ?? "—" }} · 签收 {{ r.received_at ?? "—" }}
            </div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-drawer>
  </div>
</template>
