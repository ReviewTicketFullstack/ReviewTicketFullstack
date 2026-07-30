import { useState } from 'react';
import { Ticket, Settings } from 'lucide-react';
import { MyInfoModal } from '@/pages/customer/MyInfoModal/MyInfoModal';

export function Header() {
  const [isMyInfoModalOpen, setIsMyInfoModalOpen] = useState(false);

  return (
    <>
      <header className="sticky top-0 z-40 h-14 border-b border-neutral-200 bg-neutral-50">
        <div className="flex h-full items-center justify-between px-4">
          <h1 className="font-bold">리뷰티켓</h1>
          <div className="flex items-center gap-3">
            <button
              type="button"
              className="rounded-lg p-2 hover:bg-neutral-100 active:bg-neutral-200"
              aria-label="Tickets"
            >
              <Ticket size={20} className="text-neutral-900" />
            </button>
            <button
              type="button"
              onClick={() => setIsMyInfoModalOpen(true)}
              className="rounded-lg p-2 hover:bg-neutral-100 active:bg-neutral-200"
              aria-label="Settings"
            >
              <Settings size={20} className="text-neutral-900" />
            </button>
          </div>
        </div>
      </header>

      <MyInfoModal
        open={isMyInfoModalOpen}
        onClose={() => setIsMyInfoModalOpen(false)}
      />
    </>
  );
}
