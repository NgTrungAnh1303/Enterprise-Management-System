import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';

export default function Sidebar({ onNavigate }) {
  const { role } = useAuth();

  const getMenuItems = () => {
    switch (role) {
      case 'EQUIPMENT_USER':
        return [
          { to: '/user/dashboard', label: 'Bàn làm việc', icon: 'bi-house-door' },
          { to: '/user/equipment', label: 'Tra cứu thiết bị', icon: 'bi-cpu' },
          { to: '/user/incidents/new', label: 'Báo sự cố mới', icon: 'bi-exclamation-triangle' },
          { to: '/user/incidents', label: 'Sự cố của tôi', icon: 'bi-card-checklist' },
        ];
      case 'TECHNICIAN':
        return [
          { to: '/technician/dashboard', label: 'Bàn làm việc', icon: 'bi-speedometer2' },
          { to: '/technician/work-orders', label: 'Phiếu bảo trì', icon: 'bi-tools' },
          { to: '/technician/incidents', label: 'Xử lý sự cố', icon: 'bi-wrench' },
          { to: '/technician/equipment', label: 'Thiết bị liên quan', icon: 'bi-cpu' },
        ];
      case 'MANAGER':
        return [
          { to: '/manager/dashboard', label: 'Dashboard & Cảnh báo', icon: 'bi-graph-up' },
          { to: '/manager/equipment', label: 'Quản lý thiết bị', icon: 'bi-cpu' },
          { to: '/manager/master-data', label: 'Loại TB & Vị trí', icon: 'bi-tags' },
          { to: '/manager/work-orders', label: 'Phiếu bảo trì (WO)', icon: 'bi-clipboard-check' },
          { to: '/manager/incidents', label: 'Sự cố kỹ thuật', icon: 'bi-exclamation-octagon' },
          { to: '/manager/maintenance-plans', label: 'Kế hoạch & Checklist', icon: 'bi-gear' },
          { to: '/manager/audit-logs', label: 'Nhật ký Audit Log', icon: 'bi-journal-text' },
        ];
      case 'ADMIN':
        return [
          { to: '/admin/dashboard', label: 'Bảng điều khiển', icon: 'bi-shield-check' },
          { to: '/admin/users', label: 'Quản lý tài khoản', icon: 'bi-people' },
          { to: '/admin/equipment-catalog', label: 'Danh mục thiết bị', icon: 'bi-cpu' },
          { to: '/admin/equipment-types', label: 'Loại thiết bị', icon: 'bi-tags' },
          { to: '/admin/locations', label: 'Vị trí lắp đặt', icon: 'bi-geo-alt' },
          { to: '/admin/audit-logs', label: 'Nhật ký Audit Log', icon: 'bi-journal-text' },
        ];
      default:
        return [];
    }
  };

  const menuItems = getMenuItems();

  return (
    <div className="d-flex flex-column h-100 p-3 text-white">
      {/* Brand Header */}
      <div className="d-flex align-items-center mb-4 px-2 pb-3 border-bottom border-secondary border-opacity-25">
        <i className="bi bi-shield-shaded fs-3 me-2" style={{ color: '#38BDF8' }}></i>
        <div>
          <div className="fw-bold fs-5 text-white lh-1">P05 - EMS</div>
          <small className="text-secondary" style={{ fontSize: '0.7rem' }}>Bảo trì & Điều phối Thiết bị</small>
        </div>
      </div>

      {/* Navigation List */}
      <nav className="ems-sidebar-nav flex-grow-1 overflow-y-auto pe-1">
        {menuItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
            onClick={onNavigate}
          >
            <i className={`bi ${item.icon}`}></i>
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>

      {/* Role Footer Info */}
      <div className="pt-3 border-top border-secondary border-opacity-25 px-2 mt-auto">
        <div className="text-secondary small mb-1" style={{ fontSize: '0.75rem' }}>Phân hệ vai trò</div>
        <span
          className="badge text-uppercase fw-semibold px-2 py-1"
          style={{ backgroundColor: 'rgba(56, 189, 248, 0.15)', color: '#38BDF8', fontSize: '0.75rem', letterSpacing: '0.5px' }}
        >
          {role || 'GUEST'}
        </span>
      </div>
    </div>
  );
}

