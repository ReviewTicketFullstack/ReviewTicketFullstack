import { useNavigate } from "react-router-dom";
import { LoginForm } from "./LoginPage";

export function CustomerLoginPage() {
  const navigate = useNavigate();

  const handleSuccess = () => {
    console.log("[CustomerLoginPage] Login successful, navigating to /home");
    navigate("/home", { replace: true });
  };

  return <LoginForm expectedRole="CUSTOMER" onSuccess={handleSuccess} />;
}
