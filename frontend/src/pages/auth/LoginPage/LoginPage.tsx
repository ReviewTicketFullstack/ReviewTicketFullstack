import { useState } from "react";
import { Button } from "@/shared/ui/Button";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@/app/providers";
import { login } from "@/api/authApi";
import { ApiError } from "@/shared/api";
import { InputHelperText } from "@/shared/ui/InputHelperText";
import { ForgotPasswordModal } from "./ForgotPasswordModal";
import type { FormEvent } from "react";

export function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [isForgotPasswordOpen, setIsForgotPasswordOpen] = useState(false);

  const navigate = useNavigate();

  const { signin } = useAuth();

  const handleLogin = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    console.log("[Login] Form submitted", { email });
    setError("");
    setIsLoading(true);

    try {
      console.log("[Login] Calling login API");
      const result = await login({
        email,
        password,
      });
      console.log("[Login] Login API succeeded", {
        userId: result.userId,
        role: result.role,
        expiresInSeconds: result.expiresInSeconds,
      });

      // 로그인 응답에는 이메일이 없다. 방금 입력한 값을 그대로 쓴다.
      console.log("[Login] Calling signin to restore session");
      await signin(result, email);
      console.log("[Login] signin completed successfully");

      if (result.role === "CUSTOMER") {
        console.log("[Login] Navigating to /home");
        navigate("/home");
      }

      if (result.role === "OWNER") {
        console.log("[Login] Navigating to /stores");
        navigate("/stores");
      }
    } catch (err) {
      console.error("[Login] Login failed", {
        name: err instanceof ApiError ? err.name : "Unknown",
        message: err instanceof ApiError ? err.message : String(err),
        status: err instanceof ApiError ? err.status : undefined,
      });
      // 잠금·차단 안내는 서버 문구가 그대로 보여야 한다 (401 과 429 가 다르다).
      setError(
        err instanceof ApiError ? err.message : "로그인 정보를 확인해주세요.",
      );
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex flex-col gap-8 px-5 py-8">
      <form onSubmit={handleLogin} className="flex flex-col gap-3">
        <div className="flex flex-col gap-2">
          <label htmlFor="email" className="text-xs font-semibold text-ink-900">
            이메일
          </label>
          <input
            id="email"
            type="email"
            placeholder="이메일을 입력하세요"
            value={email}
            onChange={(e) => {
              setEmail(e.target.value);
              setError("");
            }}
            className="h-11 rounded-lg border border-line-100 px-3 py-2 text-sm placeholder-ink-500 focus:border-brand-800 focus:outline-none"
            required
          />
        </div>

        <div className="flex flex-col gap-2">
          <label
            htmlFor="password"
            className="text-xs font-semibold text-ink-900"
          >
            비밀번호
          </label>
          <input
            id="password"
            type="password"
            placeholder="비밀번호를 입력하세요"
            value={password}
            onChange={(e) => {
              setPassword(e.target.value);
              setError("");
            }}
            className="h-11 rounded-lg border border-line-100 px-3 py-2 text-sm placeholder-ink-500 focus:border-brand-800 focus:outline-none"
            required
          />
        </div>

        {error && <InputHelperText variant="error">{error}</InputHelperText>}

        <Button type="submit" fullWidth size="large" disabled={isLoading}>
          {isLoading ? "로그인 중..." : "로그인하기"}
        </Button>
      </form>

      <div className="flex items-center justify-between">
        <button
          type="button"
          onClick={() => navigate("/signup")}
          className="flex-1 text-center text-sm text-ink-700 hover:text-brand-800"
        >
          회원가입하기
        </button>
        <div className="h-5 border-r border-line-100" />
        <button
          type="button"
          onClick={() => setIsForgotPasswordOpen(true)}
          className="flex-1 text-center text-sm text-ink-700 hover:text-brand-800"
        >
          비밀번호찾기
        </button>
      </div>

      <ForgotPasswordModal
        open={isForgotPasswordOpen}
        onClose={() => setIsForgotPasswordOpen(false)}
      />
    </div>
  );
}
