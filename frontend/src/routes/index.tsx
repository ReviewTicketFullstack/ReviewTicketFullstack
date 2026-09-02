import { Routes, Route, Navigate } from "react-router-dom";
import { CustomerLayout, OwnerLayout, AuthLayout } from "@/shared/layout";
import { useAuth } from "@/app/providers";
import { Loading } from "@/shared/ui";
import type { UserRole } from "@/entities/user";

import {
  HomePage,
  OrderPage,
  StoreReviewPage,
  OrderHistoryPage,
  ReviewsPage,
  OnboardingPage,
  CustomerLoginPage,
  OwnerLoginPage,
  SignUpPage,
  EmailVerificationPage,
  StoreManagementPage,
  MenuManagementPage,
  ReviewManagementPage,
} from "@/pages";

/** 역할별 첫 화면. 자기 역할이 아닌 곳으로 들어오면 이쪽으로 돌려보낸다. */
function homePathOf(role: UserRole) {
  return role === "OWNER" ? "/stores" : "/home";
}

function ProtectedRoute({
  children,
  role,
}: {
  children: React.ReactNode;
  /** 이 화면을 볼 수 있는 역할 */
  role: UserRole;
}) {
  const { user, isRestoring } = useAuth();

  // 저장된 토큰으로 사용자를 불러오는 중이다. 여기서 로그인 화면으로 보내면
  // 새로고침할 때마다 로그아웃된 것처럼 보인다.
  if (isRestoring) {
    return <Loading />;
  }

  if (!user) {
    return <Navigate to="/onboarding" replace />;
  }

  // 역할이 맞지 않으면 화면을 그리지 않는다. 서버도 역할을 검사해 403 으로
  // 막지만, 그것만 믿으면 화면은 멀쩡히 뜬 채로 요청만 실패해
  // 사장이 고객 화면에서 오류를 보게 된다.
  if (user.role !== role) {
    return <Navigate to={homePathOf(user.role)} replace />;
  }

  return children;
}

function LoginRedirectRoute({ children }: { children: React.ReactNode }) {
  const { user, isRestoring } = useAuth();

  if (isRestoring) {
    return <Loading />;
  }

  if (user) {
    return <Navigate to={homePathOf(user.role)} replace />;
  }

  return children;
}

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/onboarding" replace />} />

      <Route element={<AuthLayout />}>
        <Route
          path="/onboarding"
          element={
            <LoginRedirectRoute>
              <OnboardingPage />
            </LoginRedirectRoute>
          }
        />
        <Route
          path="/login/customer"
          element={
            <LoginRedirectRoute>
              <CustomerLoginPage />
            </LoginRedirectRoute>
          }
        />
        <Route
          path="/login/owner"
          element={
            <LoginRedirectRoute>
              <OwnerLoginPage />
            </LoginRedirectRoute>
          }
        />
        <Route path="/signup" element={<SignUpPage />} />
        <Route path="/email-verification" element={<EmailVerificationPage />} />
      </Route>

      <Route
        element={
          <ProtectedRoute role="CUSTOMER">
            <CustomerLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/home" element={<HomePage />} />
        <Route path="/order/:storeId" element={<OrderPage />} />
        <Route path="/order/:storeId/reviews" element={<StoreReviewPage />} />
        <Route path="/order-history" element={<OrderHistoryPage />} />
        <Route path="/reviews" element={<ReviewsPage />} />
      </Route>

      <Route
        element={
          <ProtectedRoute role="OWNER">
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
