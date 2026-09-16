import { useAuthStore } from "../stores/auth";

export interface ApiErrorBody {
  success?: boolean;
  code?: string;
  message?: string;
}

/** 统一请求封装：统一 /api 前缀、JWT、错误体解析、401 处理。 */
export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set("Content-Type", "application/json");
  const auth = useAuthStore();
  if (auth.token) {
    headers.set("Authorization", `Bearer ${auth.token}`);
  }

  let res: Response;
  try {
    res = await fetch(path.startsWith("/api") ? path : `/api${path}`, { ...options, headers });
  } catch (networkError) {
    throw new RequestError("NETWORK_ERROR", "无法连接后端服务，请确认容器已启动");
  }

  if (res.status === 204) {
    return undefined as T;
  }

  const text = await res.text();
  const body = text ? (JSON.parse(text) as ApiErrorBody | T) : undefined;

  if (!res.ok) {
    const errBody = body as ApiErrorBody;
    if (res.status === 401) {
      auth.clear();
    }
    throw new RequestError(errBody?.code ?? "HTTP_ERROR", errBody?.message ?? `请求失败（${res.status}）`);
  }
  return body as T;
}

export class RequestError extends Error {
  code: string;
  constructor(code: string, message: string) {
    super(message);
    this.name = "RequestError";
    this.code = code;
  }
}
