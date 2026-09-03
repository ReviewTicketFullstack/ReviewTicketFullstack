import { useNavigate } from "react-router-dom";
import { LoginForm } from "./LoginPage";

export function OwnerLoginPage() {
  const navigate = useNavigate();

  const handleSuccess = () => {
    console.log("[OwnerLoginPage] Login successful, navigating to /stores");
    navigate("/stores", { replace: true });
  };

  return <LoginForm expectedRole="OWNER" onSuccess={handleSuccess} />;
}
