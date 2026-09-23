import React from 'react';
import { Outlet } from 'react-router-dom';
import { LayoutDashboard, ClipboardList, Users, ArrowLeftRight, CreditCard, BadgeCheck, BarChart3, Bell, UserCircle } from 'lucide-react';
import DashboardLayout from '../../components/layout/DashboardLayout';

const InchargeLayout = () => {
  const menuItems = [
    { path: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { path: '/applications', label: 'Applications', icon: ClipboardList },
    { path: '/students', label: 'Students', icon: Users },
    { path: '/transfers', label: 'Transfers', icon: ArrowLeftRight },
    { path: '/fees', label: 'Fees', icon: CreditCard },
    { path: '/passes', label: 'Passes', icon: BadgeCheck },
    { path: '/reports', label: 'Reports', icon: BarChart3 },
    { path: '/notifications', label: 'Notifications', icon: Bell },
    { path: '/profile', label: 'Profile', icon: UserCircle },
  ];

  return (
    <DashboardLayout menuItems={menuItems} basePath="/incharge" title="Incharge Portal">
      <Outlet />
    </DashboardLayout>
  );
};

export default InchargeLayout;
