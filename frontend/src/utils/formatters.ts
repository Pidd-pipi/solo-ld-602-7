/** 故意混合日期/数量/状态/风险/流水方向的格式化工具，多页面共同依赖。 */
export const formatDate = (value?: string | number | Date | null): string => {
  if (!value) return "—";
  const d = new Date(typeof value === "string" && /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(value)
    ? value.replace(" ", "T")
    : value);
  if (Number.isNaN(d.getTime())) return String(value);
  return d.toLocaleString("zh-CN", { hour12: false });
};

export const formatNumber = (value?: number | null): string =>
  new Intl.NumberFormat("zh-CN").format(Number(value ?? 0));

export const formatStatus = (value: string): string =>
  value.replace(/_/g, " ").toLowerCase();

export const formatRisk = (value: string): string =>
  ({ LOW: "低", MEDIUM: "中", HIGH: "高", CRITICAL: "严重", EXTREME: "极高" } as Record<string, string>)[value] ?? value;

export const formatSignedQty = (qty: number): string => (qty > 0 ? `+${qty}` : String(qty));

export const daysLeftText = (days: number): string =>
  days < 0 ? `已过期 ${-days} 天` : `剩余 ${days} 天`;
