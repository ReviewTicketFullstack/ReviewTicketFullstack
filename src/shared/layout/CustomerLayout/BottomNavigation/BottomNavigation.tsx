import { NavLink } from 'react-router-dom';

export function BottomNavigation() {
  return (
    <nav className="sticky bottom-0 flex h-16 border-t bg-white">
      <NavLink 
        to="/home" 
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