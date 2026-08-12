import { request } from "@/shared/api";

/**
 * POST /api/uploads 응답. 어느 표에도 대응하지 않는다 — 이 요청은 행을 만들지
 * 않고 파일만 남기기 때문이다. 돌려받은 url 을 가게 로고 수정에 그대로 실어 보낸다.
 */
export interface UploadResult {
  url: string;
  width: number;
  height: number;
}

/**
 * 이미지를 올리고 주소를 돌려받는다. 서버가 긴 변 1920px 로 줄여 저장한다.
 *
 * 리뷰 사진은 이 요청을 쓰지 않는다 — AI 검증을 통과한 사진만 남겨야 하는데,
 * 미리 올려 두면 판정에 실패한 사진이 주인 없는 파일로 디스크에 쌓인다.
 *
 * @param minLongEdge 긴 변의 하한(px). 메뉴 표본 사진처럼 화질이 AI 판정에
 *                    영향을 주는 자리에서만 넘긴다. 로고나 목록 썸네일은
 *                    작아도 되므로 넘기지 않는다.
 */
export function uploadImage(
  file: File,
  minLongEdge?: number,
): Promise<UploadResult> {
  const form = new FormData();
  form.append("file", file);

  return request<UploadResult>("/uploads", {
    method: "POST",
    body: form,
    auth: true,
    query: minLongEdge ? { minLongEdge: String(minLongEdge) } : undefined,
  });
}
