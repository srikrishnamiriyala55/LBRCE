import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import { ToastProvider } from './components/common/Toast';

// Layouts
import StudentLayout from './pages/student/StudentLayout';
import InchargeLayout from './pages/incharge/InchargeLayout';
import AdminLayout from './pages/admin/AdminLayout';

// Student Pages
import StudentDashboardPage from './pages/student/StudentDashboardPage';
import StudentProfilePage from './pages/student/StudentProfilePage';
import BusApplicationPage from './pages/student/BusApplicationPage';
import ApplicationHistoryPage from './pages/student/ApplicationHistoryPage';
import FeeViewPage from './pages/student/FeeViewPage';
import BusPassPage from './pages/student/BusPassPage';
import TransferPage from './pages/student/TransferPage';
import NotificationsPage from './pages/student/NotificationsPage';

// Incharge Pages
import InchargeDashboardPage from './pages/incharge/InchargeDashboardPage';
import ApplicationManagementPage from './pages/incharge/ApplicationManagementPage';
import StudentListPage from './pages/incharge/StudentListPage';
import TransferManagementPage from './pages/incharge/TransferManagementPage';
import InchargeDataPage from './pages/incharge/InchargeDataPage';
import InchargeReportsPage from './pages/incharge/InchargeReportsPage';
import InchargeProfilePage from './pages/incharge/InchargeProfilePage';

// Admin Pages
import AdminDashboardPage from './pages/admin/AdminDashboardPage';
import BusManagementPage from './pages/admin/BusManagementPage';
import BoardingPointManagementPage from './pages/admin/BoardingPointManagementPage';
import InchargeManagementPage from './pages/admin/InchargeManagementPage';
import StudentManagementPage from './pages/admin/StudentManagementPage';
import AdminApplicationsPage from './pages/admin/AdminApplicationsPage';
import FeeManagementPage from './pages/admin/FeeManagementPage';
import AcademicYearPage from './pages/admin/AcademicYearPage';
import AuditLogsPage from './pages/admin/AuditLogsPage';
import AdminDataPage from './pages/admin/AdminDataPage';
import AdminReportsPage from './pages/admin/AdminReportsPage';
import AdminProfilePage from './pages/admin/AdminProfilePage';

function App() {
  return (
    <ToastProvider>
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<Login />} />

        {/* Student Routes */}
        <Route path="/student" element={<ProtectedRoute allowedRoles={['STUDENT']}><StudentLayout /></ProtectedRoute>}>
          <Route path="dashboard" element={<StudentDashboardPage />} />
          <Route path="profile" element={<StudentProfilePage />} />
          <Route path="apply" element={<BusApplicationPage />} />
          <Route path="applications" element={<ApplicationHistoryPage />} />
          <Route path="fees" element={<FeeViewPage />} />
          <Route path="pass" element={<BusPassPage />} />
          <Route path="transfers" element={<TransferPage />} />
          <Route path="notifications" element={<NotificationsPage />} />
          <Route path="" element={<Navigate to="/student/dashboard" replace />} />
        </Route>

        {/* Incharge Routes */}
        <Route path="/incharge" element={<ProtectedRoute allowedRoles={['INCHARGE']}><InchargeLayout /></ProtectedRoute>}>
          <Route path="dashboard" element={<InchargeDashboardPage />} />
          <Route path="applications" element={<ApplicationManagementPage />} />
          <Route path="students" element={<StudentListPage />} />
          <Route path="transfers" element={<TransferManagementPage />} />
          <Route path="fees" element={<InchargeDataPage type="fees" />} />
          <Route path="passes" element={<InchargeDataPage type="passes" />} />
          <Route path="notifications" element={<InchargeDataPage type="notifications" />} />
          <Route path="reports" element={<InchargeReportsPage />} />
          <Route path="profile" element={<InchargeProfilePage />} />
          <Route path="" element={<Navigate to="/incharge/dashboard" replace />} />
        </Route>

        {/* Admin Routes */}
        <Route path="/admin" element={<ProtectedRoute allowedRoles={['ADMIN']}><AdminLayout /></ProtectedRoute>}>
          <Route path="dashboard" element={<AdminDashboardPage />} />
          <Route path="buses" element={<BusManagementPage />} />
          <Route path="boarding-points" element={<BoardingPointManagementPage />} />
          <Route path="incharges" element={<InchargeManagementPage />} />
          <Route path="students" element={<StudentManagementPage />} />
          <Route path="applications" element={<AdminApplicationsPage />} />
          <Route path="fees" element={<FeeManagementPage />} />
          <Route path="academic-years" element={<AcademicYearPage />} />
          <Route path="audit-logs" element={<AuditLogsPage />} />
          <Route path="transfers" element={<AdminDataPage type="transfers" />} />
          <Route path="payments" element={<AdminDataPage type="payments" />} />
          <Route path="passes" element={<AdminDataPage type="passes" />} />
          <Route path="notifications" element={<AdminDataPage type="notifications" />} />
          <Route path="reports" element={<AdminReportsPage />} />
          <Route path="profile" element={<AdminProfilePage />} />
          <Route path="" element={<Navigate to="/admin/dashboard" replace />} />
        </Route>

        {/* Catch all */}
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </ToastProvider>
  );
}

export default App;
