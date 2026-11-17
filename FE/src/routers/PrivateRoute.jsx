import React from 'react';
import { Navigate, useLocation, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import LoadingState from '../components/common/LoadingState';

export default function PrivateRoute({ allowedRoles, children }) {
  const { user, isAuthenticated, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return <LoadingState fullPage message="Đang kiểm tra quyền truy cập..." />;
  }

  if (!isAuthenticated) {
    const currentPath = location.pathname + location.search;
    const safeReturnUrl = currentPath.startsWith('/') && !currentPath.startsWith('//')
      ? encodeURIComponent(currentPath)
      : '';
    const loginPath = safeReturnUrl ? `/login?returnUrl=${safeReturnUrl}` : '/login';
    return <Navigate to={loginPath} replace />;
  }

  if (allowedRoles && allowedRoles.length > 0) {
    const rolesArray = Array.isArray(allowedRoles) ? allowedRoles : [allowedRoles];
    if (!rolesArray.includes(user?.role)) {
      // Role not allowed for this route -> Redirect to 403 Forbidden page
      return <Navigate to="/403" replace />;
    }
  }

  return children ? children : <Outlet />;
}

