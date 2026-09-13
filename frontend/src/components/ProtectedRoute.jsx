import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const ProtectedRoute = ({ children, allowedRoles }) => {
  const { user, isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    // Redirect to respective dashboard if unauthorized for this role
    switch (user.role) {
      case 'STUDENT': return <Navigate to="/student/dashboard" replace />;
      case 'INCHARGE': return <Navigate to="/incharge/dashboard" replace />;
      case 'ADMIN': return <Navigate to="/admin/dashboard" replace />;
      default: return <Navigate to="/login" replace />;
    }
  }

  return children;
};

export default ProtectedRoute;
