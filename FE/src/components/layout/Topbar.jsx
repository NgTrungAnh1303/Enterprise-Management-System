import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import StatusBadge from '../common/StatusBadge';

export default function Topbar({ onToggleSidebar }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const userInitial = (user?.fullName || user?.username || 'U').charAt(0).toUpperCase();

  return (
    <header className="ems-topbar px-3 px-lg-4 d-flex align-items-center justify-content-between">
      {/* Left side: Hamburger button on mobile */}
      <div className="d-flex align-items-center">
        <button
          className="btn btn-sm btn-outline-secondary d-lg-none me-2 p-1 px-2"
          onClick={onToggleSidebar}
          aria-label="Toggle navigation menu"
        >
          <i className="bi bi-list fs-5"></i>
        </button>
        <div className="d-none d-sm-block text-secondary small fw-medium">
          Hệ thống Quản lý & Điều phối Bảo trì Thiết bị
        </div>
      </div>

      {/* Right side: User info pill & Logout */}
      <div className="d-flex align-items-center gap-3">
        <div className="d-flex align-items-center gap-2">
          <div
            className="rounded-circle d-flex align-items-center justify-content-center bg-primary bg-opacity-10 text-primary fw-bold flex-shrink-0"
            style={{ width: '34px', height: '34px', fontSize: '0.85rem' }}
          >
            {userInitial}
          </div>
          <div className="text-start d-none d-sm-block">
            <div className="fw-semibold text-dark small lh-1">{user?.fullName || user?.username}</div>
            <small className="text-muted" style={{ fontSize: '0.7rem' }}>@{user?.username}</small>
          </div>
          <StatusBadge type="role" status={user?.role} />
        </div>

        <div className="vr d-none d-sm-block" style={{ height: '24px' }}></div>

        <button
          onClick={handleLogout}
          className="btn btn-outline-danger btn-sm d-flex align-items-center gap-1"
          title="Đăng xuất khỏi hệ thống"
        >
          <i className="bi bi-box-arrow-right"></i>
          <span className="d-none d-md-inline">Đăng xuất</span>
        </button>
      </div>
    </header>
  );
}

