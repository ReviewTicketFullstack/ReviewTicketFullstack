import { useState } from "react";
import { Button } from "@/shared/ui/Button";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@/app/providers";
import { login } from "@/api/authApi";
import type { FormEvent } from "react";

export function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const navigate = useNavigate();

  const { signin } = useAuth();

  const handleLogin = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    try {
      const user = await login({
        email,
        password,
      });

      signin(user);

      if (user.role === "CUSTOMER") {
        navigate("/home");
      }

      if (user.role === "OWNER") {
        navigate("/stores");
      }
    } catch (error) {
      alert("로그인 정보를 확인해주세요.");
    }
  };

  // TODO: 인증 로직 구현. useAuth()의 authType(CUSTOMER/OWNER)에 따라
  // 로그인 API를 분기하고, 응답으로 받은 user를 signin()에 전달한다.

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
            onChange={(e) => setEmail(e.target.value)}
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
            onChange={(e) => setPassword(e.target.value)}
            className="h-11 rounded-lg border border-line-100 px-3 py-2 text-sm placeholder-ink-500 focus:border-brand-800 focus:outline-none"
            required
          />
        </div>

        <Button type="submit" fullWidth size="large">
          로그인하기
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
          className="flex-1 text-center text-sm text-ink-700 hover:text-brand-800"
        >
          비밀번호찾기
        </button>
      </div>
    </div>
  );
}
