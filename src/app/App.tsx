import { BrowserRouter } from 'react-router-dom';
import { AppRoutes } from '@/routes';

export function App() {
  return (
    <BrowserRouter>
    <div className="min-h-screen bg-gray-100">

      <AppRoutes />
    </div>
    </BrowserRouter>
  );
}
