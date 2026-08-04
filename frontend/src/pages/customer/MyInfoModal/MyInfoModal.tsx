import { useEffect, useState } from "react";
import { Modal } from "@/shared/ui/Modal/Modal";
import { Button } from "@/shared/ui";
import { InputHelperText } from "@/shared/ui/InputHelperText";
import { useAuth } from "@/app/providers";
import { changeDisplayName } from "@/api/accountApi";
import { checkName, requestPasswordReset } from "@/api/authApi";
import { ApiError } from "@/shared/api";
import { ArrowLeft } from "lucide-react";

export interface MyInfoModalProps {
  open: boolean;
  onClose: () => void;
}

export function MyInfoModal({ open, onClose }: MyInfoModalProps) {
  const { user, updateDisplayName } = useAuth();
  const [nickname, setNickname] = useState(user?.displayName ?? "");
  const [nameChecked, setNameChecked] = useState<boolean | null>(null);
  const [nameMessage, setNameMessage] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [saveError, setSaveError] = useState("");
  const [passwordMessage, setPasswordMessage] = useState("");
  const [isSendingReset, setIsSendingReset] = useState(false);

  // 모달을 다시 열면 서버가 알고 있는 이름에서 시작한다.
  useEffect(() => {
    if (!open) return;

    setNickname(user?.displayName ?? "");
    setNameChecked(null);
    setNameMessage("");
    setSaveError("");
    setPasswordMessage("");
  }, [open, user?.displayName]);

  const isNameChanged = nickname.trim().length > 0 && nickname !== user?.displayName;
  // 이름을 바꾸지 않았다면 중복확인 없이도 닫을 수 있어야 한다.
  const canSave = !isNameChanged || nameChecked === true;

  const handleDuplicateCheck = async () => {
    if (!isNameChanged) return;

    try {
      const data = await checkName(nickname.trim());
      setNameChecked(data.available);
      if (data.available) {
        setNameMessage("사용할 수 있습니다");
      } else {
        setNameMessage("이미 사용 중인 이름입니다.");
      }
    } catch (error) {
      setNameChecked(null);
      setNameMessage(
        error instanceof ApiError
          ? error.message
          : "이름 확인 중 오류가 발생했습니다.",
      );
    }
  };

  const handleSave = async () => {
    if (!isNameChanged) {
      onClose();
      return;
    }

    setIsSaving(true);
    setSaveError("");

    try {
      const result = await changeDisplayName(nickname.trim());
      updateDisplayName(result.displayName);
      onClose();
    } catch (error) {
      setSaveError(
        error instanceof ApiError ? error.message : "저장하지 못했습니다.",
      );
    } finally {
      setIsSaving(false);
    }
  };

  /**
   * 로그인한 상태에서 바로 비밀번호를 바꾸는 API 는 없다.
   * 서버는 메일 링크로 본인을 한 번 더 확인하는 재설정 흐름만 제공한다.
   */
  const handlePasswordReset = async () => {
    if (!user?.email) return;

    setIsSendingReset(true);
    setPasswordMessage("");

    try {
      const response = await requestPasswordReset(user.email);
      setPasswordMessage(response.message);
    } catch (error) {
      setPasswordMessage(
        error instanceof ApiError
          ? error.message
          : "재설정 메일을 보내지 못했습니다.",
      );
    } finally {
      setIsSendingReset(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose}>
      {/* Header */}
      <div className="flex items-center justify-between border-b border-line-100 pb-5 mb-6">
        <button
          type="button"
          onClick={onClose}
          className="rounded-lg p-2 hover:bg-fill-100 active:bg-fill-100"
          aria-label="뒤로가기"
        >
          <ArrowLeft size={20} className="text-ink-900" />
        </button>
        <h2 className="flex-1 text-center font-bold text-base">내 정보</h2>
        <div className="w-10" />
      </div>

      {/* Body */}
      <div className="space-y-6">
        {/* Nickname Section */}
        <div>
          <label className="block text-sm font-semibold text-ink-900 mb-2">
            닉네임
          </label>
          <div className="flex gap-2">
            <input
              type="text"
              value={nickname}
              onChange={(e) => {
                setNickname(e.target.value);
                setNameChecked(null);
                setNameMessage("");
                setSaveError("");
              }}
              placeholder="닉네임을 입력하세요"
              className="flex-1 px-3 py-2 border border-line-100 rounded-lg text-base focus:outline-none focus:border-brand-800 text-ink-900"
            />
            <Button
              variant="secondary"
              size="medium"
              onClick={handleDuplicateCheck}
              disabled={!isNameChanged}
            >
              중복확인
            </Button>
          </div>
          {nameMessage && (
            <div className="mt-2">
              <InputHelperText variant={nameChecked ? "info" : "error"}>
                {nameMessage}
              </InputHelperText>
            </div>
          )}
          {saveError && (
            <div className="mt-2">
              <InputHelperText variant="error">{saveError}</InputHelperText>
            </div>
          )}
        </div>

        {/* Password Section */}
        <div className="space-y-3">
          <label className="block text-sm font-semibold text-ink-900">
            비밀번호 변경
          </label>
          <p className="text-xs text-ink-700 leading-relaxed">
            본인 확인을 위해 가입 이메일로 재설정 링크를 보내드려요.
          </p>
          <Button
            variant="secondary"
            size="medium"
            fullWidth
            onClick={handlePasswordReset}
            disabled={isSendingReset || !user?.email}
          >
            {isSendingReset ? "보내는 중..." : "비밀번호 재설정 메일 받기"}
          </Button>
          {passwordMessage && (
            <InputHelperText>{passwordMessage}</InputHelperText>
          )}
        </div>

        {/* Ticket Information */}
        <div className="bg-fill-100 rounded-lg px-4 py-3">
          <p className="text-xs text-ink-700 mb-1">남은 티켓</p>
          <p className="text-base font-semibold text-ink-900">3개</p>
        </div>
      </div>

      {/* Footer */}
      <div className="mt-8 pt-6 border-t border-line-100">
        <Button
          variant="primary"
          size="large"
          fullWidth
          onClick={handleSave}
          disabled={!canSave || isSaving}
        >
          {isSaving ? "저장 중..." : "저장"}
        </Button>
      </div>
    </Modal>
  );
}
