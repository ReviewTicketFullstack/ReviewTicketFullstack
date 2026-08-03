import { Routes, Route, Navigate } from "react-router-dom";
import { CustomerLayout, OwnerLayout, AuthLayout } from "@/shared/layout";
import { useAuth } from "@/app/providers";

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
  const { user } = useAuth();

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
