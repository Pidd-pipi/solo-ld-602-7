import { computed, ref, type Ref } from "vue";

/** 极简分页 hook，列表页与审批列表共用。 */
export function usePagination<T>(rows: Ref<T[]>, pageSize = 8) {
  const page = ref(1);
  const totalPages = computed(() => Math.max(1, Math.ceil(rows.value.length / pageSize)));
  const pageRows = computed(() =>
    rows.value.slice((page.value - 1) * pageSize, page.value * pageSize)
  );

  const go = (p: number) => {
    page.value = Math.min(Math.max(1, p), totalPages.value);
  };
  const prev = () => go(page.value - 1);
  const next = () => go(page.value + 1);

  return { page, pageSize, pageRows, totalPages, go, prev, next };
}
