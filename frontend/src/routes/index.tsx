import { Routes, Route, Navigate } from "react-router-dom";
import { CustomerLayout, OwnerLayout, AuthLayout } from "@/shared/layout";
import { useAuth } from "@/app/providers";
import { Loading } from "@/shared/ui";

import {
  HomePage,
  OrderPage,
  OrderHistoryPage,
  OnboardingPage,
  LoginPage,
  SignUpPage,
  EmailVerificationPage,
  StoreManagementPage,
  MenuManagementPage,
  ReviewManagementPage,
} from "@/pages";

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { user, isRestoring } = useAuth();

  // 저장된 토큰으로 사용자를 불러오는 중이다. 여기서 로그인 화면으로 보내면
  // 새로고침할 때마다 로그아웃된 것처럼 보인다.
  if (isRestoring) {
    return <Loading />;
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return children;
}

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/onboarding" replace />} />

      <Route element={<AuthLayout />}>
        <Route path="/onboarding" element={<OnboardingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignUpPage />} />
        <Route path="/email-verification" element={<EmailVerificationPage />} />
        {/* <Route path="/signup/customer" element={<SignUpPage />} />
        <Route path="/signup/owner" element={<SignUpPage />} /> */}
      </Route>

      <Route
        element={
          <ProtectedRoute>
            <CustomerLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/home" element={<HomePage />} />
        <Route path="/order/:storeId" element={<OrderPage />} />
        <Route path="/order-history" element={<OrderHistoryPage />} />
      </Route>

      <Route
        element={
          <ProtectedRoute>
            <OwnerLayout />
          </ProtectedRoute>
        }
      >

        <Route path="stores" element={<StoreManagementPage />} />
        <Route path="menu" element={<MenuManagementPage />} />
        <Route path="reviews" element={<ReviewManagementPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/onboarding" replace />} />
    </Routes>
  );
}
