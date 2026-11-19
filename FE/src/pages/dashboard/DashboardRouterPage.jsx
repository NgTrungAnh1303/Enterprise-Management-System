import React from 'react';
import { useAuth } from '../../auth/AuthContext';
import UserDashboard from './UserDashboard';
import TechnicianDashboard from './TechnicianDashboard';
import ManagerDashboard from './ManagerDashboard';
import AdminDashboard from './AdminDashboard';
import LoadingState from '../../components/common/LoadingState';

export default function DashboardRouterPage() {
  const { role, loading } = useAuth();

  if (loading) {
    return <LoadingState message="Đang nạp bàn làm việc..." />;
  }

  switch (role) {
    case 'EQUIPMENT_USER':
      return <UserDashboard />;
    case 'TECHNICIAN':
      return <TechnicianDashboard />;
    case 'MANAGER':
      return <ManagerDashboard />;
    case 'ADMIN':
      return <AdminDashboard />;
    default:
      return <div className="p-4 text-center text-muted">Không xác định được vai trò người dùng.</div>;
  }
}

