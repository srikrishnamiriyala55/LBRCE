import React from 'react';
import { NavLink } from 'react-router-dom';

const Sidebar = ({ menuItems, isOpen, basePath, onNavigate }) => {
  return (
    <aside
      aria-label="Role navigation"
      className={`fixed md:sticky top-16 bottom-0 left-0 z-40 w-72 max-w-[85vw] md:w-64 md:h-[calc(100vh-4rem)] shrink-0 bg-white border-r border-gray-200 transform transition-transform duration-300 ease-in-out ${
        isOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'
      }`}
    >
      <div className="h-full px-4 py-6 overflow-y-auto">
        <div className="space-y-2">
          {menuItems.map((item, index) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={index}
                to={`${basePath}${item.path}`}
                onClick={onNavigate}
                end={item.path === '/dashboard'}
                className={({ isActive }) =>
                  `sidebar-link ${isActive ? 'active' : ''}`
                }
              >
                <Icon size={20} />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
