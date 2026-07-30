import type { ReactNode } from "react";

export interface ModalProps {
  open: boolean;
  onClose?: () => void;
  children?: ReactNode;
}

export function Modal({ open, onClose, children }: ModalProps) {
  if (!open) return null;

  return (
    // z-50을 추가해 화면 최상단에 뜨도록 보장합니다
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* 배경: bg-black/50을 추가해 주변을 어둡게 만듭니다 */}
      <div className="absolute inset-0 bg-black/50" onClick={onClose} />

      {/* 콘텐츠 상자: 흰 배경, 패딩, 라운딩, 그림자 효과를 줍니다 */}
      <div className="relative z-10 bg-white p-6 rounded-2xl shadow-xl max-w-md w-full mx-4">
        {children}
      </div>
    </div>
  );
}
