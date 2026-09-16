import { computed, type Ref } from "vue";
import type { InventoryBatch } from "../types/InventoryBatch";
import { availableOf } from "../types/InventoryBatch";

export interface ExpireRow extends InventoryBatch {
  daysLeft: number;
  nearExpire: boolean;
  expired: boolean;
}

/** 临期/过期预警：按到期日升序，阈值天数可配，过期排最前。 */
export function useExpireWarning(batches: Ref<InventoryBatch[]>, thresholdDays = 30) {
  const enriched = computed<ExpireRow[]>(() => {
    const now = Date.now();
    const dayMs = 24 * 3600 * 1000;
    return batches.value
      .filter((b) => b.expire_at)
      .map((b) => {
        const daysLeft = Math.ceil((new Date(String(b.expire_at).replace(" ", "T")).getTime() - now) / dayMs);
        return {
          ...b,
          daysLeft,
          nearExpire: daysLeft >= 0 && daysLeft <= thresholdDays,
          expired: daysLeft < 0
        };
      })
      .filter((b) => b.nearExpire || b.expired)
      .sort((a, b) => a.daysLeft - b.daysLeft);
  });

  const expiringCount = computed(() => enriched.value.filter((b) => b.nearExpire).length);
  const expiredCount = computed(() => enriched.value.filter((b) => b.expired).length);

  /** 临期批次仍可被占用的总量，供申请时提示。 */
  const nearExpireAvailable = computed(() =>
    enriched.value.filter((b) => b.nearExpire).reduce((s, b) => s + availableOf(b), 0));

  return { enriched, expiringCount, expiredCount, nearExpireAvailable };
}
