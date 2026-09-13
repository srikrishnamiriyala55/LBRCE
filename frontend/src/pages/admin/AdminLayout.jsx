import React from 'react';
import { Outlet } from 'react-router-dom';
import { LayoutDashboard, Bus, Map, MapPin, UserCheck, Users, ClipboardList, CreditCard, Calendar, Shield, ArrowLeftRight, Ticket, MessageSquare, Bell, BarChart3, UserCircle } from 'lucide-react';
import DashboardLayout from '../../components/layout/DashboardLayout';

const AdminLayout = () => {
  const menuItems = [
    { path: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { path: '/buses', label: 'Buses', icon: Bus },
    { path: '/routes', label: 'Routes', icon: Map },
    { path: '/boarding-points', label: 'Boarding Points & Fees', icon: MapPin },
    { path: '/incharges', label: 'Incharges', icon: UserCheck },
    { path: '/students', label: 'Students', icon: Users },
    { path: '/applications', label: 'Applications', icon: ClipboardList },
    { path: '/fees', label: 'Fees', icon: CreditCard },
    { path: '/payments', label: 'Payments', icon: CreditCard },
    { path: '/passes', label: 'Passes', icon: Ticket },
    { path: '/transfers', label: 'Transfers', icon: ArrowLeftRight },
    { path: '/complaints', label: 'Complaints', icon: MessageSquare },
    { path: '/notifications', label: 'Notifications', icon: Bell },
    { path: '/reports', label: 'Reports', icon: BarChart3 },
    { path: '/academic-years', label: 'Academic Years', icon: Calendar },
    { path: '/audit-logs', label: 'Audit Logs', icon: Shield },
    { path: '/profile', label: 'Profile', icon: UserCircle },
  ];

  return (
    <DashboardLayout menuItems={menuItems} basePath="/admin" title="Admin Portal">
      <Outlet />
    </DashboardLayout>
  );
};

export default AdminLayout;
