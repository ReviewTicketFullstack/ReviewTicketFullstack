import { useNavigate } from "react-router-dom";
import { useAuth } from "@/app/providers";
import { SignUpForm, type SignUpFormData } from "../SignUpForm";
import { signUp } from "@/shared/api/api";
import { ApiError } from "@/shared/api";
import { InputHelperText } from "@/shared/ui/InputHelperText";
import { useEffect, useState } from "react";

type SignupErrorCode =
  | "EMAIL_TOO_LONG"
  | "EMAIL_DOMAIN_INVALID"
  | "NAME_REQUIRED"
  | "NAME_TOO_LONG"
  | "PASSWORD_TOO_SHORT"
  | "PASSWORD_TOO_LONG"
  | "PASSWORD_INVALID_CHAR"
  | "PASSWORD_MISSING_UPPER"
  | "PASSWORD_MISSING_LOWER"
  | "PASSWORD_MISSING_DIGIT"
  | "PASSWORD_MISSING_SPECIAL"
  | "PASSWORD_MISMATCH"
  | "EMAIL_TAKEN"
  | "NAME_TAKEN";

function isSignupErrorCode(value: string): value is SignupErrorCode {
  return [
    "EMAIL_TOO_LONG",
    "EMAIL_DOMAIN_INVALID",
    "NAME_REQUIRED",
    "NAME_TOO_LONG",
    "PASSWORD_TOO_SHORT",
    "PASSWORD_TOO_LONG",
    "PASSWORD_INVALID_CHAR",
    "PASSWORD_MISSING_UPPER",
    "PASSWORD_MISSING_LOWER",
    "PASSWORD_MISSING_DIGIT",
    "PASSWORD_MISSING_SPECIAL",
    "PASSWORD_MISMATCH",
    "EMAIL_TAKEN",
    "NAME_TAKEN",
  ].includes(value);
}

function getErrorMessage(
  errorCode: SignupErrorCode,
  selectedRole: string,
): string {
  const baseMessages = {
    EMAIL_TOO_LONG: "이메일이 너무 깁니다",
    EMAIL_DOMAIN_INVALID: "유효하지 않은 이메일 주소입니다",
    NAME_REQUIRED:
      selectedRole === "CUSTOMER"
        ? "닉네임을 입력해 주세요"
        : "가게 이름을 입력해 주세요",
    NAME_TOO_LONG: "이름은 14자 이하여야 합니다",
    PASSWORD_TOO_SHORT: "비밀번호는 6자 이상이어야 합니다",
    PASSWORD_TOO_LONG: "비밀번호는 14자 이하여야 합니다",
    PASSWORD_INVALID_CHAR: "비밀번호에 쓸 수 없는 문자가 있습니다",
    PASSWORD_MISSING_UPPER: "비밀번호에 대문자가 필요합니다",
    PASSWORD_MISSING_LOWER: "비밀번호에 소문자가 필요합니다",
    PASSWORD_MISSING_DIGIT: "비밀번호에 숫자가 필요합니다",
    PASSWORD_MISSING_SPECIAL: "비밀번호에 특수문자가 필요합니다",
    PASSWORD_MISMATCH: "비밀번호가 서로 다릅니다",
    EMAIL_TAKEN: "이미 가입된 이메일입니다",
    NAME_TAKEN:
      selectedRole === "CUSTOMER"
        ? "이미 쓰이고 있는 닉네임입니다"
        : "이미 쓰이고 있는 가게 이름입니다",
  };

  return baseMessages[errorCode] || "회원가입 중 오류가 발생했습니다.";
}

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

    // TODO: remove debug logs
    const requestUrl = "/api/auth/signup";
    const requestBody = {
      email: data.email,
      password: "***",
      passwordConfirm: "***",
      role: selectedRole,
      displayName:
        (selectedRole === "CUSTOMER" ? data.nickname : data.storeName) ?? "",
    };

    console.groupCollapsed("[Signup] Request");
    console.log("URL:", requestUrl);
    console.log("Body (passwords masked):", requestBody);
    console.log("Actual password length:", data.password.length);
    console.groupEnd();

    try {
      const response = await signUp({
        email: data.email,
        password: data.password,
        passwordConfirm: data.passwordConfirm,
        role: selectedRole,
        displayName:
          (selectedRole === "CUSTOMER" ? data.nickname : data.storeName) ?? "",
      });

      // TODO: remove debug logs
      console.groupCollapsed("[Signup] Success Response");
      console.log("Status: 200 OK");
      console.log("Response body:", response);
      console.log("Email:", response.email);
      console.log("Message:", response.message);
      console.groupEnd();

      // 서버가 정규화한 이메일(소문자·공백 제거)을 그대로 들고 간다.
      navigate(
        `/email-verification?email=${encodeURIComponent(response.email)}`,
        {
          replace: true,
        },
      );
    } catch (err) {
      // TODO: remove debug logs
      if (err instanceof ApiError) {
        console.groupCollapsed("[Signup] Error Response");
        console.log("Status:", err.status);
        console.log("Error code:", err.errorCode);
        console.log("Raw message:", err.message);

        const errorCode = err.errorCode;

        if (!errorCode || !isSignupErrorCode(errorCode)) {
          setError("회원가입 중 오류가 발생했습니다.");
          return;
        }

        const uiMessage = getErrorMessage(errorCode, selectedRole);
        console.log("Parsed errorCode:", errorCode);
        console.log("Mapped UI message:", uiMessage);
        console.groupEnd();

        setError(uiMessage);
      } else {
        console.groupCollapsed("[Signup] Unexpected Error");
        console.log("Error type:", err instanceof Error ? "Error" : typeof err);
        console.log("Error details:", err);
        console.groupEnd();
      }
    }
  };

  return (
    <div className="flex flex-col gap-8 px-5 py-8">
      {error && <InputHelperText variant="error">{error}</InputHelperText>}
      <SignUpForm authType={selectedRole} onSubmit={handleSubmit} />
    </div>
  );
}
