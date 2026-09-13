import React, { useEffect, useState } from 'react';
import { Outlet } from 'react-router-dom';
import { LayoutDashboard, User, FileText, ClipboardList, CreditCard, Ticket, ArrowLeftRight, MessageSquare, Bell } from 'lucide-react';
import DashboardLayout from '../../components/layout/DashboardLayout';
import api from '../../utils/axios';

const StudentLayout = () => {
  const [capabilities, setCapabilities] = useState(null);
  useEffect(() => {
    api.get('/student/dashboard').then(r => setCapabilities(r.data.data || r.data))
      .catch(() => setCapabilities({ canApply: false, canTransfer: false, canComplain: false }));
  }, []);
  const allocated = capabilities?.transportationStatus === 'ALLOCATED';
  const menuItems = [
    { path: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { path: '/profile', label: 'Profile', icon: User },
    ...(capabilities?.canApply ? [{ path: '/apply', label: 'Apply for Bus', icon: FileText }] : []),
    { path: '/applications', label: 'Applications', icon: ClipboardList },
    ...(allocated ? [{ path: '/fees', label: 'Fees', icon: CreditCard }, { path: '/pass', label: 'Bus Pass', icon: Ticket }] : []),
    ...(allocated ? [{ path: '/transfers', label: 'Transfers', icon: ArrowLeftRight }] : []),
    ...(capabilities?.canComplain ? [{ path: '/complaints', label: 'Complaints', icon: MessageSquare }] : []),
    { path: '/notifications', label: 'Notifications', icon: Bell },
  ];

  return (
    <DashboardLayout menuItems={menuItems} basePath="/student" title="Student Portal">
      <Outlet />
    </DashboardLayout>
  );
};

export default StudentLayout;
