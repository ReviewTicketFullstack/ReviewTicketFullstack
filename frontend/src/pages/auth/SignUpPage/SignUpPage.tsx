import { useNavigate } from "react-router-dom";
import { useAuth } from "@/app/providers";
import { SignUpForm, type SignUpFormData } from "../SignUpForm";
import type { UserRole } from "@/entities/user";
import { useEffect } from "react";

export interface SignUpPageProps {
  authType: UserRole;
}

export function SignUpPage() {
  const navigate = useNavigate();
  const { selectedRole } = useAuth();

  useEffect(() => {
    if (!selectedRole) {
      navigate("/onboarding", { replace: true });
    }
  }, [selectedRole, navigate]);

  if (!selectedRole) {
    return null;
  }

  const handleSubmit = async (data: SignUpFormData) => {
    try {
      const payload = {
        email: data.email,
        password: data.password,
        role: selectedRole,
        ...(selectedRole === "CUSTOMER" && { nickname: data.nickname }),
        ...(selectedRole === "OWNER" && { businessName: data.businessName }),
      };

      const response = await fetch("/api/auth/signup", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        throw new Error("회원가입 실패");
      }

      alert("회원가입 성공! 로그인해주세요.");
      navigate("/login", { replace: true });
    } catch (error) {
      alert(
        error instanceof Error
          ? error.message
          : "회원가입 중 오류가 발생했습니다.",
      );
    }
  };

  return (
    <div className="flex flex-col gap-8 px-5 py-8">
      <SignUpForm authType={selectedRole} onSubmit={handleSubmit} />
    </div>
  );
}
