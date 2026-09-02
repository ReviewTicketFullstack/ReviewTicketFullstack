import { useEffect, useState } from "react";
import { useNavigate, useLocation, Navigate } from "react-router-dom";
import { Button } from "@/shared/ui";
import { InputHelperText } from "@/shared/ui/InputHelperText";
import { getVerificationStatus, resendVerification } from "@/api/authApi";
import { useAuth } from "@/app/providers";
import { ApiError } from "@/shared/api";
import { Mail, MailCheck } from "lucide-react";

/** 비밀번호 재설정 시 메일 인증 관련 로직 */
const STATUS_POLL_INTERVAL_MS = 5000;

export function EmailVerificationPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { selectedRole } = useAuth();

  const emailParam = new URLSearchParams(location.search).get("email");
  const email = emailParam ? decodeURIComponent(emailParam) : "";

  const [isVerified, setIsVerified] = useState(false);
  const [isResending, setIsResending] = useState(false);
  const [resendMessage, setResendMessage] = useState("");
  const [resendError, setResendError] = useState("");

  useEffect(() => {
    if (!email || isVerified) return;

    const controller = new AbortController();

    const poll = async () => {
      try {
        const status = await getVerificationStatus(email, controller.signal);
        if (status.verified) setIsVerified(true);
      } catch {
        // 일시적인 실패는 무시한다. 다음 주기에 다시 물어본다.
      }
    };

    poll();
    const timer = window.setInterval(poll, STATUS_POLL_INTERVAL_MS);

    return () => {
      controller.abort();
      window.clearInterval(timer);
    };
  }, [email, isVerified]);

  if (!emailParam) {
    return <Navigate to="/signup" replace />;
  }

  const handleResend = async () => {
    setIsResending(true);
    setResendError("");
    setResendMessage("");

    try {
      await resendVerification(email);
      setResendMessage("인증 메일을 다시 보냈습니다.");
    } catch (error) {
      // 짧은 시간에 여러 번 누르면 서버가 429 로 막는다. 그 문구를 그대로 보여준다.
      setResendError(
        error instanceof ApiError
          ? error.message
          : "인증 메일을 보내지 못했습니다.",
      );
    } finally {
      setIsResending(false);
    }
  };

  return (
    <div className="flex flex-col gap-8 px-5 py-8">
      <div className="flex flex-col items-center gap-6 py-6">
        <div className="flex items-center justify-center w-16 h-16 rounded-full bg-brand-50">
          {isVerified ? (
            <MailCheck size={32} className="text-brand-800" />
          ) : (
            <Mail size={32} className="text-brand-800" />
          )}
        </div>

        <div className="flex flex-col gap-4 text-center">
          <h1 className="text-2xl font-bold text-ink-900">
            {isVerified ? "인증이 완료되었습니다." : "인증 메일을 보냈습니다."}
          </h1>
          <p className="text-sm text-ink-700 leading-relaxed">
            {isVerified ? (
              <>
                이제 로그인할 수 있어요.
                <br />
                가입한 이메일과 비밀번호로 로그인해 주세요.
              </>
            ) : (
              <>
                이메일의 인증 링크를 클릭하면
                <br />
                회원가입이 완료됩니다.
              </>
            )}
          </p>
        </div>
      </div>

      <div className="flex flex-col gap-3 rounded-lg bg-fill-50 p-4">
        <p className="text-xs font-semibold text-ink-600">등록된 이메일</p>
        <p className="text-sm font-semibold text-ink-900 break-all">{email}</p>
      </div>

      <div className="flex flex-col gap-2">
        <Button
          fullWidth
          size="large"
          onClick={() => {
            if (selectedRole) {
              const loginPath = selectedRole === "CUSTOMER" ? "/login/customer" : "/login/owner";
              navigate(loginPath, { replace: true });
            }
          }}
        >
          로그인하기
        </Button>

        {!isVerified && (
          <>
            <Button
              variant="secondary"
              fullWidth
              size="large"
              onClick={handleResend}
              disabled={isResending}
            >
              {isResending ? "보내는 중..." : "인증 메일 다시 받기"}
            </Button>

            {resendMessage && (
              <InputHelperText>{resendMessage}</InputHelperText>
            )}
            {resendError && (
              <InputHelperText variant="error">{resendError}</InputHelperText>
            )}
          </>
        )}

        <button
          type="button"
          onClick={() => navigate("/signup", { replace: true })}
          className="py-3 text-sm text-center text-ink-600 hover:text-brand-800 transition-colors"
        >
          다시 입력하기
        </button>
      </div>

      {!isVerified && (
        <div className="text-center">
          <p className="text-xs text-ink-500 leading-relaxed">
            인증 메일이 오지 않으셨나요?
            <br />
            스팸함을 확인하거나 잠시 후 다시 시도해주세요.
          </p>
        </div>
      )}
    </div>
  );
}
