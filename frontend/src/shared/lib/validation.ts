// 비번 정규식 조건
export const passwordRegex =
  /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[!@#$%^&*])[\w!@#$%^&*]{6,14}$/;

// 비번 일치 함수
export function validatePassword(password: string): boolean {
  return passwordRegex.test(password);
}

// 이메일 형태 검증 문자@문자.문자
export function validateEmail(email: string): boolean {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}
