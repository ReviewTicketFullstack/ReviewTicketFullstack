import { useState, type FormEvent } from 'react';
import { Input, Button } from '@/shared/ui';
import { validatePassword, validateEmail } from '@/shared/lib/validation';
import type { UserRole } from '@/entities/user';

export interface SignUpFormProps {
  authType: UserRole;
  onSubmit: (data: SignUpFormData) => Promise<void>;
}

export interface SignUpFormData {
  email: string;
  password: string;
  passwordConfirm: string;
  nickname?: string;
  businessName?: string;
}

export function SignUpForm({ authType, onSubmit }: SignUpFormProps) {
  const [email, setEmail] = useState('');
  const [emailDuplicated, setEmailDuplicated] = useState<boolean | null>(null);
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [nickname, setNickname] = useState('');
  const [businessName, setBusinessName] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const isPasswordValid = validatePassword(password);
  const isPasswordConfirmed = password === passwordConfirm && password.length > 0;
  const isEmailValid = validateEmail(email);
  const isBusinessNameOrNicknameValid =
    authType === 'CUSTOMER' ? nickname.length > 0 : businessName.length > 0;

  const isFormValid =
    isEmailValid &&
    isPasswordValid &&
    isPasswordConfirmed &&
    isBusinessNameOrNicknameValid &&
    emailDuplicated === false;

  const handleDuplicateCheck = () => {
    if (!isEmailValid) return;
    setEmailDuplicated(false);
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
        ...(authType === 'CUSTOMER' && { nickname }),
        ...(authType === 'OWNER' && { businessName }),
      });
    } finally {
      setIsLoading(false);
    }
  };

  const greetingText =
    authType === 'CUSTOMER' ? '고객님' : '사장님';

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

        <Input
          type="password"
          placeholder="비밀번호"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="new-password"
          helperText="영어, 숫자, 특수문자를 포함하여 6자리 이상 15자리 미만으로 입력해주세요"
          showHelperText={password.length > 0 && !isPasswordValid}
          error={password.length > 0 && !isPasswordValid ? '비밀번호 형식이 맞지 않습니다.' : undefined}
        />

        <Input
          type="password"
          placeholder="비밀번호 확인"
          value={passwordConfirm}
          onChange={(e) => setPasswordConfirm(e.target.value)}
          autoComplete="new-password"
          error={
            passwordConfirm.length > 0 && password !== passwordConfirm
              ? '비밀번호가 일치하지 않습니다.'
              : undefined
          }
        />

        {authType === 'CUSTOMER' ? (
          <Input
            placeholder="닉네임"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            autoComplete="nickname"
          />
        ) : (
          <Input
            placeholder="상호명을 입력해주세요"
            value={businessName}
            onChange={(e) => setBusinessName(e.target.value)}
          />
        )}
      </div>

      <Button
        type="submit"
        fullWidth
        size="large"
        disabled={!isFormValid || isLoading}
      >
        {isLoading ? '회원가입 중...' : '회원가입하기'}
      </Button>
    </form>
  );
}
