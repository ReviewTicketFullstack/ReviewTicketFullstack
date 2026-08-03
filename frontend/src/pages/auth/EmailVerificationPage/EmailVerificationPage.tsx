import { useNavigate, useLocation, Navigate } from "react-router-dom";
import { Button } from "@/shared/ui";
import { Mail } from "lucide-react";

export function EmailVerificationPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const email = new URLSearchParams(location.search).get("email");

  if (!email) {
    return <Navigate to="/signup" replace />;
  }

  return (
    <div className="flex flex-col gap-8 px-5 py-8">
      <div className="flex flex-col items-center gap-6 py-6">
        <div className="flex items-center justify-center w-16 h-16 rounded-full bg-brand-50">
          <Mail size={32} className="text-brand-800" />
        </div>

        <div className="flex flex-col gap-4 text-center">
          <h1 className="text-2xl font-bold text-ink-900">
            인증 메일을 보냈습니다.
          </h1>
          <p className="text-sm text-ink-700 leading-relaxed">
            이메일의 인증 링크를 클릭하면
            <br />
            회원가입이 완료됩니다.
          </p>
        </div>
      </div>

      <div className="flex flex-col gap-3 rounded-lg bg-fill-50 p-4">
        <p className="text-xs font-semibold text-ink-600">등록된 이메일</p>
        <p className="text-sm font-semibold text-ink-900 break-all">
          {decodeURIComponent(email)}
        </p>
      </div>

      <div className="flex flex-col gap-2">
        <Button fullWidth size="large" onClick={() => navigate("/login")}>
          로그인하기
        </Button>
        <button
          type="button"
          onClick={() => navigate("/signup", { replace: true })}
          className="py-3 text-sm text-center text-ink-600 hover:text-brand-800 transition-colors"
        >
          다시 입력하기
        </button>
      </div>

      <div className="text-center">
        <p className="text-xs text-ink-500 leading-relaxed">
          인증 메일이 오지 않으셨나요?
          <br />
          스팸함을 확인하거나 잠시 후 다시 시도해주세요.
        </p>
      </div>
    </div>
  );
}