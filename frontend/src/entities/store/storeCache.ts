/**
 * 홈 화면 가게 목록의 localStorage 캐시 키와 무효화 함수.
 *
 * HomePage 가 자정까지 유효하다고 보고 캐시를 쓰는데, 개발 중 DB가
 * 재구성되면(가게 표를 통째로 다시 만드는 경우) 캐시 속 번호와 실제 번호가
 * 어긋날 수 있다 — 캐시는 그대로인데 서버 값만 바뀌기 때문이다. 이때는
 * OrderPage 가 그 어긋난 번호로 상세 조회를 시도하다 404를 받게 되므로,
 * 그 지점에서 이 함수로 캐시를 지워 다음 홈 진입 때 새로 받아오게 한다.
 */
const STORE_CACHE_KEY = "stores_cache";

export function clearStoreCache() {
  localStorage.removeItem(STORE_CACHE_KEY);
}

export { STORE_CACHE_KEY };
