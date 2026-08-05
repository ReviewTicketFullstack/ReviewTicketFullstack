import { useNavigate } from "react-router-dom";
import { useAuth } from "@/app/providers";
import { SignUpForm, type SignUpFormData } from "../SignUpForm";
import { signUp } from "@/api/authApi";
import { ApiError } from "@/shared/api";
import { InputHelperText } from "@/shared/ui/InputHelperText";
import { useEffect, useState } from "react";

export function SignUpPage() {
  const navigate = useNavigate();
  const { selectedRole } = useAuth();
  const [error, setError] = useState("");

  useEffect(() => {
    if (!selectedRole) {
      navigate("/onboarding", { replace: true });
    }
  }, [selectedRole, navigate]);

  if (!selectedRole) {
    return null;
  }

  const handleSubmit = async (data: SignUpFormData) => {
    setError("");

    try {
      const response = await signUp({
        email: data.email,
        password: data.password,
        passwordConfirm: data.passwordConfirm,
        role: selectedRole,
        displayName:
          (selectedRole === "CUSTOMER" ? data.nickname : data.storeName) ?? "",
      });

      // 서버가 정규화한 이메일(소문자·공백 제거)을 그대로 들고 간다.
      navigate(`/email-verification?email=${encodeURIComponent(response.email)}`, {
        replace: true,
      });
    } catch (err) {
      // 비밀번호 규칙 위반, 이미 가입된 이메일 등 사유가 서버 문구에 담겨 온다.
      setError(
        err instanceof ApiError ? err.message : "회원가입 중 오류가 발생했습니다.",
      );
    }
  };

  return (
    <div className="flex flex-col gap-8 px-5 py-8">
      {error && <InputHelperText variant="error">{error}</InputHelperText>}
      <SignUpForm authType={selectedRole} onSubmit={handleSubmit} />
    </div>
  );
}
