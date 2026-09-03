import { useNavigate } from "react-router-dom";
import { Utensils, Store } from "lucide-react";
import { useAuth } from "@/app/providers";

export function OnboardingPage() {
  const { setUserRole } = useAuth();
  const navigate = useNavigate();

  const handleCustomerClick = () => {
    setUserRole("CUSTOMER");
    navigate("/login");
  };

  const handleOwnerClick = () => {
    setUserRole("OWNER");
    navigate("/login");
  };

  return (
    <div className="flex flex-col min-h-screen px-5 pb-5 pt-8">
      {/* Hero Section */}
      <div
        className="flex-1 flex flex-col items-center justify-center gap-8 opacity-0"
        style={{ animation: "fadeIn 0.8s ease-out forwards" }}
      >
        {/* Logo Placeholder */}
        <div className="flex items-center justify-center w-20 h-20 rounded-2xl bg-brand-50">
          <div className="text-5xl">🎫</div>
        </div>

        {/* App Name */}
        <div className="text-center">
          <h1 className="text-3xl font-bold text-ink-900 mb-3">
            Review Ticket
          </h1>
          <p className="text-sm text-ink-700 leading-relaxed">
            주문하고 리뷰를 작성하면
            <br />
            보상을 받을 수 있습니다.
          </p>
        </div>

        {/* Illustration Placeholder */}
        <div className="w-full max-w-xs aspect-square rounded-2xl bg-brand-50 flex items-center justify-center gap-2">
          <span className="text-6xl">🍔</span>
          <span className="text-6xl">✍️</span>
          <span className="text-6xl">🎫</span>
        </div>
      </div>

      {/* Role Selection Cards */}
      <div className="flex flex-col gap-3">
        {/* Customer Card */}
        <button
          type="button"
          onClick={handleCustomerClick}
          className="rounded-2xl bg-surface border border-line-100 p-4 transition-all duration-300 ease-out hover:shadow-raised active:scale-95 cursor-pointer opacity-0"
          style={{
            animation: "slideUp 0.6s ease-out forwards",
            animationDelay: "200ms",
          }}
        >
          <div className="flex items-start gap-3">
            <div className="flex items-center justify-center w-14 h-14 rounded-xl bg-brand-50 flex-shrink-0">
              <Utensils size={28} className="text-brand-800" />
            </div>
            <div className="flex-1 text-left">
              <h2 className="text-base font-bold text-ink-900 mb-1">
                고객님으로 로그인할게요!
              </h2>
              <p className="text-xs text-ink-700 leading-relaxed">
                주문하고 리뷰를 작성해
                <br />
                다양한 혜택을 받아보세요.
              </p>
            </div>
          </div>
        </button>

        {/* Owner Card */}
        <button
          type="button"
          onClick={handleOwnerClick}
          className="rounded-2xl bg-surface border border-line-100 p-4 transition-all duration-300 ease-out hover:shadow-raised active:scale-95 cursor-pointer opacity-0"
          style={{
            animation: "slideUp 0.6s ease-out forwards",
            animationDelay: "300ms",
          }}
        >
          <div className="flex items-start gap-3">
            <div className="flex items-center justify-center w-14 h-14 rounded-xl bg-brand-50 flex-shrink-0">
              <Store size={28} className="text-brand-800" />
            </div>
            <div className="flex-1 text-left">
              <h2 className="text-base font-bold text-ink-900 mb-1">
                사장님으로 로그인할게요!
              </h2>
              <p className="text-xs text-ink-700 leading-relaxed">
                리뷰 이벤트를 운영하고
                <br />
                매장을 관리해보세요.
              </p>
            </div>
          </div>
        </button>
      </div>

      {/* Footer */}
      <div
        className="mt-8 text-center opacity-0"
        style={{ animation: "fadeIn 0.8s ease-out forwards 400ms" }}
      >
        <p className="text-xs text-ink-500 leading-relaxed">
          서비스를 이용하면 이용약관 및<br />
          개인정보처리방침에 동의한 것으로
          <br />
          간주됩니다.
        </p>
      </div>
    </div>
  );
}
