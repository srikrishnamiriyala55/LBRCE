import React, { useState } from 'react';
import Navbar from '../common/Navbar';
import Sidebar from '../common/Sidebar';

const DashboardLayout = ({ menuItems, basePath, title, children }) => {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  const toggleSidebar = () => {
    setIsSidebarOpen(!isSidebarOpen);
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Navbar title={title} onMenuToggle={toggleSidebar} />
      
      <div className="flex flex-1 min-h-0">
        <Sidebar menuItems={menuItems} isOpen={isSidebarOpen} basePath={basePath} onNavigate={() => setIsSidebarOpen(false)} />
        
        {/* Overlay for mobile sidebar */}
        {isSidebarOpen && (
          <div 
            className="fixed inset-0 bg-gray-600 bg-opacity-50 z-30 md:hidden"
            onClick={() => setIsSidebarOpen(false)}
          />
        )}
        
        <main className="min-w-0 flex-1 overflow-y-auto overflow-x-hidden px-3 py-4 sm:p-6 lg:p-8">
          <div className="w-full max-w-screen-2xl mx-auto space-y-4 sm:space-y-6">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
};

export default DashboardLayout;
