import { useNavigate } from "react-router-dom";
import { ChevronLeft } from "lucide-react";

export interface AuthHeaderProps {
  title: string;
}

export function AuthHeader({ title }: AuthHeaderProps) {
  const navigate = useNavigate();

  return (
    <header className="sticky top-0 z-40 h-14 border-b border-line-100 bg-surface">
      <div className="flex h-full items-center justify-between px-5">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="flex items-center justify-center rounded-lg p-2 hover:bg-fill-100 active:bg-line-100"
          aria-label="뒤로가기"
        >
          <ChevronLeft size={20} className="text-ink-900" />
        </button>

        <h1 className="flex-1 text-center text-xl font-bold text-ink-900">
          {title}
        </h1>

        <div className="w-10" />
      </div>
    </header>
  );
}
