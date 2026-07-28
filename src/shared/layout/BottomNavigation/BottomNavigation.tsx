import { NavLink } from 'react-router-dom';

export function BottomNavigation() {
  return (
    <nav className="fixed bottom-0 left-0 right-0 flex h-16 border-t border-gray-200 bg-white z-50">
      <NavLink 
        to="/order" 
        className={({ isActive }) => 
          `flex-1 flex items-center justify-center text-sm ${isActive ? 'text-blue-600 font-bold' : 'text-gray-500'}`
        }
      >
        홈
      </NavLink>
      <NavLink 
        to="/order-history" 
        className={({ isActive }) => 
          `flex-1 flex items-center justify-center text-sm ${isActive ? 'text-blue-600 font-bold' : 'text-gray-500'}`
        }
      >
        주문내역
      </NavLink>
    </nav>
  );
}