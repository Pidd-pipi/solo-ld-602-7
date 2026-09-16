import { reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { DispatchOrder } from "../types/DispatchOrder";
import type { ApplyDispatchPayload } from "../types/api";
import { useDispatchStore } from "../stores/DispatchOrderStore";
import { useDashboardStore } from "../stores/DashboardStore";
import { useInventoryBatchStore } from "../stores/InventoryBatchStore";
import { useAuthStore } from "../stores/auth";
import { canDispatch, type DispatchActionName } from "../constants/permission";
import { errorText } from "../constants/errorMessages";
import { RequestError } from "../utils/request";

type ActionDef = {
  key: DispatchActionName;
  label: string;
  danger?: boolean;
  needReason?: boolean;
  success: string;
};

/** 与状态机对应的动作表，按钮显隐由 RBAC + 状态可达性共同决定。 */
const ACTION_DEFS: Record<string, ActionDef> = {
  approve: { key: "approve", label: "审批通过", success: "已审批通过（库存仍为占用，未扣减）" },
  reject: { key: "reject", label: "驳回", danger: true, needReason: true, success: "已驳回，占用已全部释放" },
  outbound: { key: "outbound", label: "出库", success: "已按批次扣减并登记去向" },
  receive: { key: "receive", label: "签收确认", success: "避难点已签收" },
  refuse: { key: "refuse", label: "拒签", danger: true, needReason: true, success: "已拒签，原批次、原数量已回补" },
  cancel: { key: "cancel", label: "撤销", danger: true, needReason: true, success: "已撤销：未出库部分释放占用，已出库部分按原批次回补" }
};

const NEXT_ACTIONS: Record<string, DispatchActionName[]> = {
  SUBMITTED: ["approve", "reject", "cancel"],
  APPROVED: ["outbound", "cancel"],
  DISPATCHED: ["receive", "refuse", "cancel"]
};

/** 调拨闭环动作编排：权限过滤、原因输入、统一报错，动作后联动刷新列表/批次/态势。 */
export function useDispatchFlow() {
  const dispatchStore = useDispatchStore();
  const dashboardStore = useDashboardStore();
  const batchStore = useInventoryBatchStore();
  const authStore = useAuthStore();
  const actingId = ref<number | null>(null);

  const refreshAll = async () => {
    await Promise.all([dispatchStore.load(), batchStore.load(), dashboardStore.load()]);
  };

  /** 当前登录角色对某单据可执行的动作（已过滤 RBAC 与状态机）。 */
  const availableActions = (order: Pick<DispatchOrder, "id" | "status">) =>
    (NEXT_ACTIONS[order.status] ?? []).filter((a) => canDispatch(authStore.role, a));

  const actionDef = (key: DispatchActionName): ActionDef => ACTION_DEFS[key];

  const runAction = async (order: DispatchOrder, key: DispatchActionName): Promise<boolean> => {
    const def = ACTION_DEFS[key];
    let reason = "";
    if (def.needReason) {
      try {
        const { value } = await ElMessageBox.prompt(`请填写${def.label}原因（调拨单 #${order.id}）`, def.label, {
          confirmButtonText: "确认",
          cancelButtonText: "取消",
          inputType: "textarea",
          inputValidator: (v) => (!!v && v.trim().length > 0 ? true : "原因为必填项")
        });
        reason = value.trim();
      } catch {
        return false; // 取消输入
      }
    }
    actingId.value = order.id;
    try {
      switch (key) {
        case "approve": await dispatchStore.approve(order.id); break;
        case "reject": await dispatchStore.reject(order.id, reason); break;
        case "outbound": await dispatchStore.outbound(order.id); break;
        case "receive": await dispatchStore.receive(order.id); break;
        case "refuse": await dispatchStore.refuse(order.id, reason); break;
        case "cancel": await dispatchStore.cancel(order.id, reason); break;
      }
      await refreshAll();
      ElMessage.success(def.success);
      return true;
    } catch (e) {
      // 业务冲突（含幂等内容冲突）优先展示后端渲染的具体原因（原单号/差异）
      ElMessage.error(e instanceof RequestError ? (e.message || errorText(e.code)) : "操作失败");
      return false;
    } finally {
      actingId.value = null;
    }
  };

  /** 提交申请：同一 requestId 重复提交由后端幂等，前端给出明确提示。 */
  const submit = async (payload: ApplyDispatchPayload): Promise<{ ok: boolean; idempotent: boolean }> => {
    try {
      const seen = dispatchStore.rows.some((r) => r.request_id === payload.requestId);
      await dispatchStore.apply(payload);
      await refreshAll();
      if (seen) {
        ElMessage.warning("该申请已提交过，已返回原调拨单，未重复占用库存");
      } else {
        ElMessage.success("申请已提交，可用库存已按批次占用");
      }
      return reactive({ ok: true, idempotent: seen });
    } catch (e) {
      ElMessage.error(e instanceof RequestError ? (e.message || errorText(e.code)) : "申请失败");
      return { ok: false, idempotent: false };
    }
  };

  return { actingId, availableActions, actionDef, runAction, submit, refreshAll };
}
