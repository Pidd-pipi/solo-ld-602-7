<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { storeToRefs } from "pinia";
import { ElMessage } from "element-plus";
import { useCatalogStore } from "../stores/CatalogStore";
import { useDispatchStore } from "../stores/DispatchOrderStore";
import { createEvent } from "../api/Event";
import StatusBadge from "../components/common/StatusBadge.vue";
import EmptyState from "../components/common/EmptyState.vue";
import { RequestError } from "../utils/request";

const catalog = useCatalogStore();
const dispatch = useDispatchStore();
const { events } = storeToRefs(catalog);

const dialogVisible = ref(false);
const form = reactive({ title: "", district: "城东区", level: "MEDIUM" });

onMounted(async () => {
  await catalog.load();
  await dispatch.load();
});

const ordersOfEvent = (eventId: number | null) =>
  dispatch.rows.filter((o) => o.event_id === eventId);

const submit = async () => {
  if (!form.title.trim()) {
    ElMessage.warning("请填写事件标题");
    return;
  }
  try {
    await createEvent({ ...form });
    ElMessage.success("灾害事件已登记");
    dialogVisible.value = false;
    form.title = "";
    await catalog.load(true);
  } catch (e) {
    ElMessage.error(e instanceof RequestError ? e.message : "登记失败");
  }
};
</script>

<template>
  <div>
    <div class="page-toolbar">
      <span class="muted small">登记灾害事件并关联调拨单，查看响应进度</span>
      <div class="spacer" />
      <el-button type="primary" size="small" @click="dialogVisible = true">登记灾害事件</el-button>
    </div>

    <div class="grid grid-2">
      <div v-for="e in events" :key="e.id" class="panel">
        <div style="display:flex;justify-content:space-between;align-items:center">
          <h3 style="margin:0">{{ e.title }}</h3>
          <el-tag size="small" :type="e.status === 'ACTIVE' ? 'danger' : 'info'">
            {{ e.status === "ACTIVE" ? "响应中" : "已关闭" }}
          </el-tag>
        </div>
        <div class="muted small" style="margin:6px 0 10px">{{ e.district }} · 等级 {{ e.level }}</div>
        <div class="small" style="margin-bottom:8px">关联调拨 {{ ordersOfEvent(e.id).length }} 单</div>
        <div v-if="ordersOfEvent(e.id).length" class="grid" style="gap:6px">
          <div v-for="o in ordersOfEvent(e.id)" :key="o.id" class="event-order">
            <span>#{{ o.id }} {{ o.warehouse_name }} → {{ o.shelter_name }}</span>
            <StatusBadge :value="o.status" />
          </div>
        </div>
        <EmptyState v-else description="暂无关联调拨" :image-size="48" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="登记灾害事件" width="440px">
      <el-form :model="form" label-width="72px">
        <el-form-item label="标题"><el-input v-model="form.title" placeholder="如：9·15 台风内涝" /></el-form-item>
        <el-form-item label="辖区">
          <el-select v-model="form.district">
            <el-option label="城东区" value="城东区" />
            <el-option label="城北区" value="城北区" />
            <el-option label="城南区" value="城南区" />
          </el-select>
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="form.level">
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
            <el-option label="严重" value="CRITICAL" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">登记</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.event-order { display: flex; justify-content: space-between; align-items: center; padding: 6px 8px; background: var(--el-fill-color-light); border-radius: 6px; font-size: 13px; }
</style>
