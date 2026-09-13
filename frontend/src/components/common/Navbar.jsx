import React from 'react';
import { Menu, LogOut } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';

const Navbar = ({ title = 'LBRCE BTMS', onMenuToggle }) => {
  const { user, logout } = useAuth();

  return (
    <nav className="bg-blue-800 text-white shadow-md sticky top-0 z-50">
      <div className="w-full px-3 sm:px-6 lg:px-8">
        <div className="flex justify-between gap-2 h-16">
          <div className="flex min-w-0 items-center gap-2 sm:gap-4">
            <button
              onClick={onMenuToggle}
              aria-label="Open navigation menu"
              className="md:hidden shrink-0 p-2 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-white"
            >
              <Menu size={24} />
            </button>
            <div className="min-w-0 flex items-center gap-2 sm:gap-3">
              <img className="h-9 w-9 sm:h-10 sm:w-10 shrink-0 bg-white rounded-full p-1" src="/logo.jpg" alt="LBRCE Logo" />
              <span className="truncate font-bold text-sm xs:text-base sm:text-xl tracking-wide">{title}</span>
            </div>
          </div>
          <div className="shrink-0 flex items-center gap-2 sm:gap-4">
            <div className="hidden sm:flex flex-col items-end">
              <span className="text-sm font-semibold">{user?.name}</span>
              <span className="text-xs bg-blue-700 px-2 py-0.5 rounded-full mt-1">
                {user?.role}
              </span>
            </div>
            <button
              onClick={logout}
              aria-label="Logout"
              className="flex items-center gap-2 bg-blue-700 hover:bg-blue-600 p-2 sm:px-3 sm:py-2 rounded-lg transition-colors text-sm font-medium"
            >
              <LogOut size={18} />
              <span className="hidden sm:block">Logout</span>
            </button>
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
