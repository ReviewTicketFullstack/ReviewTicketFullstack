import { useState } from 'react';
import { Modal } from '@/shared/ui/Modal/Modal';
import { Button } from '@/shared/ui';
import { ArrowLeft } from 'lucide-react';

export interface MyInfoModalProps {
  open: boolean;
  onClose: () => void;
}

export function MyInfoModal({ open, onClose }: MyInfoModalProps) {
  const [nickname, setNickname] = useState('사용자');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [isNicknameDuplicate, setIsNicknameDuplicate] = useState(false);

  const handleDuplicateCheck = () => {
    // TODO: API 호출로 중복 확인
    setIsNicknameDuplicate(false);
  };

  const handleSave = () => {
    // TODO: API 호출로 정보 저장
    onClose();
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
              onChange={(e) => setNickname(e.target.value)}
              placeholder="닉네임을 입력하세요"
              className="flex-1 px-3 py-2 border border-line-100 rounded-lg text-base focus:outline-none focus:border-brand-800 text-ink-900"
            />
            <Button
              variant="secondary"
              size="medium"
              onClick={handleDuplicateCheck}
            >
              중복확인
            </Button>
          </div>
          {isNicknameDuplicate && (
            <p className="text-xs text-brand-900 mt-2">이미 사용 중인 닉네임입니다.</p>
          )}
        </div>

        {/* Password Section */}
        <div className="space-y-3">
          <label className="block text-sm font-semibold text-ink-900">
            비밀번호 변경
          </label>
          <div>
            <input
              type="password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              placeholder="현재 비밀번호"
              className="w-full px-3 py-2 border border-line-100 rounded-lg text-base focus:outline-none focus:border-brand-800 text-ink-900"
            />
          </div>
          <div>
            <input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="새 비밀번호"
              className="w-full px-3 py-2 border border-line-100 rounded-lg text-base focus:outline-none focus:border-brand-800 text-ink-900"
            />
          </div>
        </div>

        {/* Ticket Information */}
        <div className="bg-fill-100 rounded-lg px-4 py-3">
          <p className="text-xs text-ink-700 mb-1">남은 티켓</p>
          <p className="text-base font-semibold text-ink-900">12개</p>
        </div>
      </div>

      {/* Footer */}
      <div className="mt-8 pt-6 border-t border-line-100">
        <Button
          variant="primary"
          size="large"
          fullWidth
          onClick={handleSave}
        >
          저장
        </Button>
      </div>
    </Modal>
  );
}
