import { Routes, Route, Navigate } from "react-router-dom";
import { CustomerLayout, OwnerLayout, AuthLayout } from "@/shared/layout";
import {
  HomePage,
  OrderPage,
  OrderHistoryPage,
  OnboardingPage,
  LoginPage,
  CustomerSignUpPage,
  OwnerSignUpPage,
  StoreManagementPage,
  MenuManagementPage,
  ReviewManagementPage,
} from "@/pages";

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/onboarding" replace />} />

      <Route element={<AuthLayout />}>
        <Route path="/onboarding" element={<OnboardingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup/customer" element={<CustomerSignUpPage />} />
        <Route path="/signup/owner" element={<OwnerSignUpPage />} />
      </Route>

      <Route element={<CustomerLayout />}>
        <Route path="/home" element={<HomePage />} />
        <Route path="/order/:storeId" element={<OrderPage />} />
        <Route path="/order-history" element={<OrderHistoryPage />} />
      </Route>

      <Route path="/owner" element={<OwnerLayout />}>
        <Route path="stores" element={<StoreManagementPage />} />
        <Route path="menu" element={<MenuManagementPage />} />
        <Route path="reviews" element={<ReviewManagementPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/onboarding" replace />} />
    </Routes>
  );
}
