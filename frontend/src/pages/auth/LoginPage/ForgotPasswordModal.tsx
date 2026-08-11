import { useState, type FormEvent } from "react";
import { Modal, Button, Input } from "@/shared/ui";
import { checkEmail, requestPasswordReset } from "@/api/authApi";
import { validateEmail } from "@/shared/lib";
import { ApiError } from "@/shared/api";
import type { UserRole } from "@/entities/user";

export interface ForgotPasswordModalProps {
  open: boolean;
  onClose: () => void;
  expectedRole: UserRole;
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

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!validateEmail(email) || isSending) return;

    setIsSending(true);
    setError("");

    try {
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
      setMessage(response.message);
      setIsResetSent(true);
    } catch (err) {
      setError(
        err instanceof ApiError
          ? err.message
          : "재설정 메일을 보내지 못했습니다.",
      );
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
