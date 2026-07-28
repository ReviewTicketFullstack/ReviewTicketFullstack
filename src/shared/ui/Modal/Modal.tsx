import type { ReactNode } from 'react';

export interface ModalProps {
  open: boolean;
  onClose?: () => void;
  children?: ReactNode;
}

export function Modal({ open, onClose, children }: ModalProps) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 flex items-center justify-center">
      <div className="absolute inset-0" onClick={onClose} />
      <div className="relative">{children}</div>
    </div>
  );
}
