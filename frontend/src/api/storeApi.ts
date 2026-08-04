import { request } from "@/shared/api";
import type { Store } from "@/entities/store";

/**
 * [TODO] GET /api/stores 명세 미확정. 아래 두 가지가 확정되면 맞춘다.
 *  - 응답이 배열 그대로인지 { stores: [...] } 래핑인지
 *  - 페이지네이션 파라미터가 붙는지
 */
export function getStores(signal?: AbortSignal): Promise<Store[]> {
  return request<Store[]>("/stores", { auth: true, signal });
}
