import { Routes, Route } from 'react-router-dom';
import { CustomerLayout, OwnerLayout, AuthLayout } from '@/shared/layout';
import {
  OrderPage,
  OrderHistoryPage,
  OnboardingPage,
  LoginPage,
  CustomerSignUpPage,
  OwnerSignUpPage,
  StoreManagementPage,
  MenuManagementPage,
  ReviewManagementPage,
} from '@/pages';

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<AuthLayout />}>
        <Route path="/onboarding" element={<OnboardingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup/customer" element={<CustomerSignUpPage />} />
        <Route path="/signup/owner" element={<OwnerSignUpPage />} />
      </Route>

      <Route element={<CustomerLayout />}>
        <Route path="/order" element={<OrderPage />} />
        <Route path="/order-history" element={<OrderHistoryPage />} />
      </Route>

      <Route path="/owner" element={<OwnerLayout />}>
        <Route path="stores" element={<StoreManagementPage />} />
        <Route path="menu" element={<MenuManagementPage />} />
        <Route path="reviews" element={<ReviewManagementPage />} />
      </Route>
    </Routes>
  );
}
