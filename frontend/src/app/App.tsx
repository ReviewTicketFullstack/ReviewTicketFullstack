import { BrowserRouter } from "react-router-dom";
import { AppRoutes } from "@/routes";
import { AuthProvider } from "./providers";

export function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="min-h-screen bg-surface-sub">
          <AppRoutes />
        </div>
      </BrowserRouter>
    </AuthProvider>
  );
}
