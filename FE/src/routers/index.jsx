import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import PrivateRoute from './PrivateRoute';
import AppLayout from '../components/layout/AppLayout';

import LandingPage from '../pages/public/LandingPage';
import LoginPage from '../pages/auth/LoginPage';
import RegisterPage from '../pages/auth/RegisterPage';
import UnauthorizedPage from '../pages/error/UnauthorizedPage';
import ForbiddenPage from '../pages/error/ForbiddenPage';
import NotFoundPage from '../pages/error/NotFoundPage';

import DashboardRouterPage from '../pages/dashboard/DashboardRouterPage';
import UserDashboard from '../pages/dashboard/UserDashboard';
import TechnicianDashboard from '../pages/dashboard/TechnicianDashboard';
import ManagerDashboard from '../pages/dashboard/ManagerDashboard';
import AdminDashboard from '../pages/dashboard/AdminDashboard';

import EquipmentListPage from '../pages/equipment/EquipmentListPage';
import EquipmentFormPage from '../pages/equipment/EquipmentFormPage';
import EquipmentDetailPage from '../pages/equipment/EquipmentDetailPage';
import MasterDataPage from '../pages/admin/MasterDataPage';
import MaintenancePlanConfigPage from '../pages/manager/MaintenancePlanConfigPage';

import WorkOrderListPage from '../pages/workorders/WorkOrderListPage';
import WorkOrderCreatePage from '../pages/workorders/WorkOrderCreatePage';
import WorkOrderExecutionPage from '../pages/workorders/WorkOrderExecutionPage';
import WorkOrderAcceptancePage from '../pages/workorders/WorkOrderAcceptancePage';

import IncidentReportPage from '../pages/incidents/IncidentReportPage';
import MyIncidentsPage from '../pages/incidents/MyIncidentsPage';
import IncidentListPage from '../pages/incidents/IncidentListPage';
import IncidentDetailPage from '../pages/incidents/IncidentDetailPage';
import EquipmentHistoryPage from '../pages/equipment/EquipmentHistoryPage';
import UserManagementPage from '../pages/admin/UserManagementPage';
import AuditLogPage from '../pages/admin/AuditLogPage';

export default function AppRoutes() {
  return (
    <Routes>
      {/* Public Routes */}
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/401" element={<UnauthorizedPage />} />
      <Route path="/403" element={<ForbiddenPage />} />
      <Route path="/404" element={<NotFoundPage />} />

      {/* Protected Routes wrapped in AppLayout */}
      <Route
        element={
          <PrivateRoute>
            <AppLayout />
          </PrivateRoute>
        }
      >
        {/* Dynamic Role Dashboard Route */}
        <Route path="/dashboard" element={<DashboardRouterPage />} />

        {/* 1. EQUIPMENT_USER Routes */}
        <Route element={<PrivateRoute allowedRoles={['EQUIPMENT_USER']} />}>
          <Route path="/user/dashboard" element={<UserDashboard />} />
          <Route path="/user/equipment" element={<EquipmentListPage />} />
          <Route path="/user/equipment/:id" element={<EquipmentDetailPage />} />
          <Route path="/user/incidents/new" element={<IncidentReportPage />} />
          <Route path="/user/incidents" element={<MyIncidentsPage />} />
          <Route path="/user/incidents/:id" element={<MyIncidentsPage />} />
        </Route>

        {/* 2. TECHNICIAN Routes */}
        <Route element={<PrivateRoute allowedRoles={['TECHNICIAN']} />}>
          <Route path="/technician/dashboard" element={<TechnicianDashboard />} />
          <Route path="/technician/work-orders" element={<WorkOrderListPage />} />
          <Route path="/technician/work-orders/:id" element={<WorkOrderExecutionPage />} />
          <Route path="/technician/incidents" element={<IncidentListPage />} />
          <Route path="/technician/incidents/:id" element={<IncidentDetailPage />} />
          <Route path="/technician/equipment" element={<EquipmentListPage />} />
          <Route path="/technician/equipment/:id" element={<EquipmentDetailPage />} />
          <Route path="/technician/equipment/:id/history" element={<EquipmentHistoryPage />} />
        </Route>

        {/* 3. MANAGER Routes */}
        <Route element={<PrivateRoute allowedRoles={['MANAGER']} />}>
          <Route path="/manager/dashboard" element={<ManagerDashboard />} />
          <Route path="/manager/equipment" element={<EquipmentListPage />} />
          <Route path="/manager/equipment/new" element={<EquipmentFormPage />} />
          <Route path="/manager/equipment/:id" element={<EquipmentDetailPage />} />
          <Route path="/manager/equipment/:id/edit" element={<EquipmentFormPage />} />
          <Route path="/manager/equipment/:id/history" element={<EquipmentHistoryPage />} />
          <Route path="/manager/master-data" element={<MasterDataPage />} />
          <Route path="/manager/equipment-types" element={<Navigate to="/manager/master-data?tab=types" replace />} />
          <Route path="/manager/locations" element={<Navigate to="/manager/master-data?tab=locations" replace />} />
          <Route path="/manager/work-orders" element={<WorkOrderListPage />} />
          <Route path="/manager/work-orders/new" element={<WorkOrderCreatePage />} />
          <Route path="/manager/work-orders/:id" element={<WorkOrderAcceptancePage />} />
          <Route path="/manager/incidents" element={<IncidentListPage />} />
          <Route path="/manager/incidents/:id" element={<IncidentDetailPage />} />
          <Route path="/manager/maintenance-plans" element={<MaintenancePlanConfigPage />} />
          <Route path="/manager/audit-logs" element={<AuditLogPage />} />
        </Route>

        {/* 4. ADMIN Routes */}
        <Route element={<PrivateRoute allowedRoles={['ADMIN']} />}>
          <Route path="/admin/dashboard" element={<AdminDashboard />} />
          <Route path="/admin/users" element={<UserManagementPage />} />
          <Route path="/admin/master-data" element={<MasterDataPage />} />
          <Route path="/admin/equipment-types" element={<Navigate to="/admin/master-data?tab=types" replace />} />
          <Route path="/admin/locations" element={<Navigate to="/admin/master-data?tab=locations" replace />} />
          <Route path="/admin/equipment-catalog" element={<EquipmentListPage />} />
          <Route path="/admin/equipment/new" element={<EquipmentFormPage />} />
          <Route path="/admin/equipment/:id" element={<EquipmentDetailPage />} />
          <Route path="/admin/equipment/:id/edit" element={<EquipmentFormPage />} />
          <Route path="/admin/audit-logs" element={<AuditLogPage />} />
        </Route>
      </Route>

      {/* Catch-all Wildcard Route */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
