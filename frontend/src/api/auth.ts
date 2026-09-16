import { request } from "../utils/request";
import type { LoginResult } from "../types/api";

export const login = (username: string, password: string): Promise<LoginResult> =>
  request<LoginResult>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password })
  });
