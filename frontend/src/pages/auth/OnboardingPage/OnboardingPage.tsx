import { useNavigate } from "react-router-dom";
import { Utensils, Store } from "lucide-react";
import { useAuth } from "@/app/providers";

export function OnboardingPage() {
  const { setSelectedRole } = useAuth();
  const navigate = useNavigate();

  const handleCustomerClick = () => {
    setSelectedRole("CUSTOMER");
    navigate("/login/customer");
  };

  const handleOwnerClick = () => {
    setSelectedRole("OWNER");
    navigate("/login/owner");
  };

  return (
    <div className="flex min-h-screen flex-col gap-8 px-5 pb-8 pt-8">
      {/* Hero Section */}
      <div
        className="flex-1 flex flex-col items-center justify-center gap-8 opacity-0"
        style={{ animation: "fadeIn 0.8s ease-out forwards" }}
      >
        {/* Logo Placeholder */}
        <div className="flex size-20 items-center justify-center rounded-2xl bg-brand-50 text-5xl">
          🎫
        </div>

        {/* App Name */}
        <div className="flex flex-col gap-3 text-center">
          <h1 className="text-3xl font-bold text-ink-900">
            리뷰<span className="text-brand-800">티켓</span>
          </h1>
          <p className="text-sm leading-relaxed text-ink-700">
            주문하고 리뷰를 남기면
            <br />
            티켓을 받을 수 있어요.
          </p>
        </div>

        {/* Illustration Placeholder */}
        <div className="flex aspect-square w-full max-w-xs items-center justify-center gap-3 rounded-2xl bg-brand-50 text-6xl">
          <span>🍔</span>
          <span>✍️</span>
          <span>🎫</span>
        </div>
      </div>

      {/* Role Selection Cards */}
      <div className="flex flex-col gap-3">
        {/* Customer Card */}
        <button
          type="button"
          onClick={handleCustomerClick}
          className="cursor-pointer rounded-2xl border border-line-100 bg-surface p-5 opacity-0 transition-all duration-200 ease-out hover:border-brand-800 hover:shadow-raised active:scale-[0.98] motion-reduce:active:scale-100"
          style={{
            animation: "slideUp 0.6s ease-out forwards",
            animationDelay: "200ms",
          }}
        >
          <div className="flex items-center gap-3">
            <div className="flex size-14 shrink-0 items-center justify-center rounded-xl bg-brand-50">
              <Utensils size={28} className="text-brand-800" />
            </div>
            <div className="flex-1 text-left">
              <h2 className="mb-1 text-base font-bold text-ink-900">
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
          className="cursor-pointer rounded-2xl border border-line-100 bg-surface p-5 opacity-0 transition-all duration-200 ease-out hover:border-brand-800 hover:shadow-raised active:scale-[0.98] motion-reduce:active:scale-100"
          style={{
            animation: "slideUp 0.6s ease-out forwards",
            animationDelay: "300ms",
          }}
        >
          <div className="flex items-center gap-3">
            <div className="flex size-14 shrink-0 items-center justify-center rounded-xl bg-brand-50">
              <Store size={28} className="text-brand-800" />
            </div>
            <div className="flex-1 text-left">
              <h2 className="mb-1 text-base font-bold text-ink-900">
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
        className="text-center opacity-0"
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
