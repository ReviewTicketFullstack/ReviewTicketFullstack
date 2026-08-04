import { useState, useEffect, type FormEvent } from "react";
import { Input, Button } from "@/shared/ui";
import { validatePassword, validateEmail } from "@/shared/lib/validation";
import type { UserRole } from "@/entities/user";

export interface SignUpFormProps {
  authType: UserRole;
  onSubmit: (data: SignUpFormData) => Promise<void>;
}

export interface SignUpFormData {
  email: string;
  password: string;
  passwordConfirm: string;
  nickname?: string;
  storeName?: string;
}

export function SignUpForm({ authType, onSubmit }: SignUpFormProps) {
  const [email, setEmail] = useState("");
  const [emailDuplicated, setEmailDuplicated] = useState<boolean | null>(null);
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [nickname, setNickname] = useState("");
  const [nicknameDuplicated, setNicknameDuplicated] = useState<boolean | null>(null);
  const [storeName, setStoreName] = useState("");
  const [storeNameDuplicated, setStoreNameDuplicated] = useState<boolean | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const isPasswordValid = validatePassword(password);
  const isPasswordConfirmed =
    password === passwordConfirm && password.length > 0;
  const isEmailValid = validateEmail(email);
  const isStoreNameOrNicknameValid =
    authType === "CUSTOMER" ? nickname.length > 0 : storeName.length > 0;

  const isNameDuplicateChecked =
    authType === "CUSTOMER"
      ? nicknameDuplicated === false
      : storeNameDuplicated === false;

  const isFormValid =
    isEmailValid &&
    isPasswordValid &&
    isPasswordConfirmed &&
    isStoreNameOrNicknameValid &&
    emailDuplicated === false &&
    isNameDuplicateChecked;

  // TODO: remove debug logs
  useEffect(() => {
    if (emailDuplicated !== null) {
      console.group('📍 SignUpForm Helper Text - Email');
      console.log('State: emailDuplicated =', emailDuplicated);
      console.log('Will render:', emailDuplicated ? '이미 가입된 이메일입니다.' : '이용 가능한 이메일입니다.');
      console.groupEnd();
    }
  }, [emailDuplicated]);

  // TODO: remove debug logs
  useEffect(() => {
    if (nicknameDuplicated !== null) {
      console.group('📍 SignUpForm Helper Text - Nickname');
      console.log('State: nicknameDuplicated =', nicknameDuplicated);
      console.log('Will render:', nicknameDuplicated ? '이미 사용 중인 이름입니다.' : '이용 가능한 이름입니다.');
      console.groupEnd();
    }
  }, [nicknameDuplicated]);

  // TODO: remove debug logs
  useEffect(() => {
    if (storeNameDuplicated !== null) {
      console.group('📍 SignUpForm Helper Text - StoreName');
      console.log('State: storeNameDuplicated =', storeNameDuplicated);
      console.log('Will render:', storeNameDuplicated ? '이미 사용 중인 이름입니다.' : '이용 가능한 이름입니다.');
      console.groupEnd();
    }
  }, [storeNameDuplicated]);

  const handleDuplicateCheck = async () => {
    if (!isEmailValid) return;

    try {
      const response = await fetch(
        `/api/auth/check-email?email=${encodeURIComponent(email)}`,
      );

      const data = await response.json();

      // TODO: remove debug logs
      console.group('🔍 check-email API Response');
      console.log('Endpoint:', 'check-email');
      console.log('Full Response:', data);
      console.log('Available:', data.available);
      console.log('Message:', data.message);
      console.groupEnd();

      setEmailDuplicated(!data.available);
    } catch (error) {
      setEmailDuplicated(null);
    }
  };

  const handleNicknameCheck = async () => {
    if (!nickname) return;

    try {
      const response = await fetch(
        `/api/auth/check-name?name=${encodeURIComponent(nickname)}`,
      );

      const data = await response.json();

      // TODO: remove debug logs
      console.group('🔍 check-name API Response (nickname)');
      console.log('Endpoint:', 'check-name');
      console.log('Full Response:', data);
      console.log('Available:', data.available);
      console.log('Message:', data.message);
      console.groupEnd();

      setNicknameDuplicated(!data.available);
    } catch (error) {
      setNicknameDuplicated(null);
    }
  };

  const handleStoreNameCheck = async () => {
    if (!storeName) return;

    try {
      const response = await fetch(
        `/api/auth/check-name?name=${encodeURIComponent(storeName)}`,
      );

      const data = await response.json();

      // TODO: remove debug logs
      console.group('🔍 check-name API Response (storeName)');
      console.log('Endpoint:', 'check-name');
      console.log('Full Response:', data);
      console.log('Available:', data.available);
      console.log('Message:', data.message);
      console.groupEnd();

      setStoreNameDuplicated(!data.available);
    } catch (error) {
      setStoreNameDuplicated(null);
    }
  };

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!isFormValid) return;

    setIsLoading(true);
    try {
      await onSubmit({
        email,
        password,
        passwordConfirm,
        ...(authType === "CUSTOMER" && { nickname }),
        ...(authType === "OWNER" && { storeName }),
      });
    } finally {
      setIsLoading(false);
    }
  };

  const greetingText = authType === "CUSTOMER" ? "고객님" : "사장님";

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-8">
      <div className="flex flex-col gap-3">
        <h1 className="text-xl font-bold text-ink-900">
          안녕하세요.
          <br />
          {greetingText}!
        </h1>
      </div>

      <div className="flex flex-col gap-5">
        <div className="flex gap-2">
          <div className="flex-1">
            <Input
              type="email"
              placeholder="이메일 주소"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                setEmailDuplicated(null);
              }}
              autoComplete="email"
            />
          </div>

          <button
            type="button"
            onClick={handleDuplicateCheck}
            disabled={!isEmailValid}
            className="h-11 rounded-lg bg-brand-800 px-4 text-xs font-semibold text-white transition-colors hover:bg-brand-950 disabled:cursor-not-allowed disabled:bg-ink-300"
          >
            중복확인
          </button>
        </div>

        {emailDuplicated === false && (
          <p className="text-xs text-green-700">이용 가능한 이메일입니다.</p>
        )}

        {emailDuplicated === true && (
          <p className="text-xs text-red-700">이미 가입된 이메일입니다.</p>
        )}

        <Input
          type="password"
          placeholder="비밀번호"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="new-password"
          helperText="영문 대/소문자, 숫자, 특수문자를 포함한 6~14자리로 입력해주세요"
          showHelperText={password.length > 0 && !isPasswordValid}
          error={getPasswordError(password)}
        />

        <Input
          type="password"
          placeholder="비밀번호 확인"
          value={passwordConfirm}
          onChange={(e) => setPasswordConfirm(e.target.value)}
          autoComplete="new-password"
          error={
            passwordConfirm.length > 0 && password !== passwordConfirm
              ? "비밀번호가 일치하지 않습니다."
              : undefined
          }
        />

        {authType === "CUSTOMER" ? (
          <>
            <div className="flex gap-2">
              <div className="flex-1">
                <Input
                  placeholder="닉네임"
                  value={nickname}
                  onChange={(e) => {
                    setNickname(e.target.value);
                    setNicknameDuplicated(null);
                  }}
                  autoComplete="nickname"
                />
              </div>

              <button
                type="button"
                onClick={handleNicknameCheck}
                disabled={!nickname}
                className="h-11 rounded-lg bg-brand-800 px-4 text-xs font-semibold text-white transition-colors hover:bg-brand-950 disabled:cursor-not-allowed disabled:bg-ink-300"
              >
                중복확인
              </button>
            </div>

            {nicknameDuplicated === false && (
              <p className="text-xs text-green-700">이용 가능한 이름입니다.</p>
            )}

            {nicknameDuplicated === true && (
              <p className="text-xs text-red-700">이미 사용 중인 이름입니다.</p>
            )}
          </>
        ) : (
          <>
            <div className="flex gap-2">
              <div className="flex-1">
                <Input
                  placeholder="상호명을 입력해주세요"
                  value={storeName}
                  onChange={(e) => {
                    setStoreName(e.target.value);
                    setStoreNameDuplicated(null);
                  }}
                />
              </div>

              <button
                type="button"
                onClick={handleStoreNameCheck}
                disabled={!storeName}
                className="h-11 rounded-lg bg-brand-800 px-4 text-xs font-semibold text-white transition-colors hover:bg-brand-950 disabled:cursor-not-allowed disabled:bg-ink-300"
              >
                중복확인
              </button>
            </div>

            {storeNameDuplicated === false && (
              <p className="text-xs text-green-700">이용 가능한 이름입니다.</p>
            )}

            {storeNameDuplicated === true && (
              <p className="text-xs text-red-700">이미 사용 중인 이름입니다.</p>
            )}
          </>
        )}
      </div>

      <Button
        type="submit"
        fullWidth
        size="large"
        disabled={!isFormValid || isLoading}
      >
        {isLoading ? "회원가입 중..." : "회원가입하기"}
      </Button>
    </form>
  );
}

export function getPasswordError(password: string): string | undefined {
  if (!password) return undefined;

  if (!/[A-Z]/.test(password)) {
    return "대문자를 포함해주세요.";
  }

  if (!/[a-z]/.test(password)) {
    return "소문자를 포함해주세요.";
  }

  if (!/\d/.test(password)) {
    return "숫자를 포함해주세요.";
  }

  if (!/[!@#$%^&*]/.test(password)) {
    return "특수문자를 포함해주세요.";
  }

  if (password.length < 6 || password.length > 14) {
    return "6~14자리로 입력해주세요.";
  }

  return undefined;
}
