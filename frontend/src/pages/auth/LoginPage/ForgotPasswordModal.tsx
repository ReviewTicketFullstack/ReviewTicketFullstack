import { useState, type FormEvent } from "react";
import { Modal, Button, Input } from "@/shared/ui";
import { checkEmail, requestPasswordReset } from "@/api/authApi";
import { validateEmail } from "@/shared/lib";
import { ApiError } from "@/shared/api";
import type { UserRole } from "@/entities/user";
import { useAuth } from "@/app/providers";

export interface ForgotPasswordModalProps {
  open: boolean;
  onClose: () => void;
  expectedRole: UserRole;
}

/**
 * 서버는 errorCode 만 보내고 문구는 화면이 채운다.
 * NO_EXISTING_EMAIL(이메일 자체가 없음)과 ROLE_MISMATCH(이메일은 있지만
 * 다른 role 계정)를 구분해서, 역할 카드를 잘못 고른 본인이 "이메일이
 * 아예 없다"고 오인하지 않게 한다.
 */
function toMessage(error: unknown): string {
  if (!(error instanceof ApiError)) return "재설정 메일을 보내지 못했습니다.";

  switch (error.errorCode) {
    case "NO_EXISTING_EMAIL":
      return "등록되지 않은 이메일입니다.";
    case "ROLE_MISMATCH":
      return "계정의 역할에 맞지 않는 접근 시도입니다.";
    default:
      return error.message;
  }
}

/**
 * 재설정 메일만 요청한다. 실제 비밀번호 변경은 메일 링크가 여는
 * 서버 페이지(GET /api/auth/password-reset)에서 이뤄진다.
 * expectedRole과 일치하는 이메일만 재설정 허용.
 *
 * 주의: 이 역할 검사는 GET /api/auth/check-email 이 role 을 함께 내려줘야 동작한다.
 * 현재 서버 AvailabilityResponse 는 available 만 담고 있어 검사가 통과만 한다.
 */
export function ForgotPasswordModal({
  open,
  onClose,
  expectedRole,
}: ForgotPasswordModalProps) {
  const [email, setEmail] = useState("");
  const [isSending, setIsSending] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [isResetSent, setIsResetSent] = useState(false);

  const { selectedRole } = useAuth();

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!validateEmail(email) || isSending) return;

    if (!selectedRole) {
      setError("어느 계정으로 찾을지 알 수 없어요. 처음 화면부터 다시 시작해 주세요.");
      return;
    }

    setIsSending(true);
    setError("");

    try {
      // checkEmail 은 role 을 가리지 않고 존재 여부만 본다 — role 이 다른
      // 계정으로 가입된 이메일은 여기선 안 걸러지고, requestPasswordReset 이
      // role 까지 맞춰 조회하며 최종적으로 막는다.
      const availabilityResponse = await checkEmail(email);
      if (availabilityResponse.available) {
        setError("등록되지 않은 이메일입니다.");
        return;
      }

      if (availabilityResponse.role && availabilityResponse.role !== expectedRole) {
        const roleLabel = expectedRole === "CUSTOMER" ? "고객" : "사장";
        setError(`이 이메일은 ${roleLabel}용 계정이 아닙니다.`);
        return;
      }

      const response = await requestPasswordReset(email);
      const response = await requestPasswordReset(email, selectedRole);
      setMessage(response.message);
      setIsResetSent(true);
    } catch (err) {
      setError(
        err instanceof ApiError
          ? err.message
          : "재설정 메일을 보내지 못했습니다.",
      );
      setError(toMessage(err));
    } finally {
      setIsSending(false);
    }
  };

  const handleClose = () => {
    setEmail("");
    setMessage("");
    setError("");
    onClose();
  };

  return (
    <Modal open={open} onClose={handleClose}>
      <h2 className="mb-2 text-base font-bold text-ink-900">비밀번호 찾기</h2>
      <p className="mb-6 text-sm text-ink-700">
        가입할 때 쓴 이메일로 재설정 링크를 보내드려요.
      </p>

      {message ? (
        <div className="flex flex-col gap-4">
          <p className="text-sm text-ink-900">{message}</p>
          <Button fullWidth size="large" onClick={handleClose}>
            확인
          </Button>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <Input
            type="email"
            placeholder="이메일 주소"
            value={email}
            onChange={(e) => {
              setEmail(e.target.value);
              setError("");
            }}
            autoComplete="email"
            error={error || undefined}
          />

          <Button
            type="submit"
            fullWidth
            size="large"
            disabled={!validateEmail(email) || isSending || isResetSent}
          >
            {isSending
              ? "보내는 중..."
              : isResetSent
                ? "메일이 전송되었습니다."
                : "재설정 메일 받기"}
          </Button>
        </form>
      )}
    </Modal>
  );
}
