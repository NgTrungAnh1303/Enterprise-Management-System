import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Navbar from 'react-bootstrap/Navbar';
import Container from 'react-bootstrap/Container';
import Button from 'react-bootstrap/Button';
import Badge from 'react-bootstrap/Badge';
import { useAuth } from '../../auth/AuthContext';

export default function PublicLayout({ children }) {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const getRoleDashboardPath = (role) => {
    switch (role) {
      case 'EQUIPMENT_USER':
        return '/user/dashboard';
      case 'TECHNICIAN':
        return '/technician/dashboard';
      case 'MANAGER':
        return '/manager/dashboard';
      case 'ADMIN':
        return '/admin/dashboard';
      default:
        return '/';
    }
  };

  const getRoleBadgeVariant = (role) => {
    switch (role) {
      case 'ADMIN':
        return 'danger';
      case 'MANAGER':
        return 'warning';
      case 'TECHNICIAN':
        return 'info';
      case 'EQUIPMENT_USER':
      default:
        return 'primary';
    }
  };

  const handleLogout = async () => {
    await logout();
    navigate('/');
  };

  return (
    <div className="min-vh-100 d-flex flex-column bg-app">
      {/* Top Navbar */}
      <Navbar bg="white" className="border-bottom border-light-subtle shadow-sm py-2 sticky-top">
        <Container fluid="xl" className="px-3 px-sm-4">
          <Navbar.Brand as={Link} to="/" className="d-flex align-items-center gap-2 text-decoration-none">
            <div
              className="d-flex align-items-center justify-content-center rounded text-white"
              style={{ width: '36px', height: '36px', backgroundColor: '#1D4ED8' }}
            >
              <i className="bi bi-shield-shaded fs-5"></i>
            </div>
            <div>
              <div className="fw-bold text-dark fs-5 lh-1">P05 - EMS</div>
              <small className="text-secondary d-none d-sm-inline" style={{ fontSize: '0.75rem' }}>
                Hệ thống Quản lý & Điều phối Bảo trì Thiết bị
              </small>
            </div>
          </Navbar.Brand>

          <div className="d-flex align-items-center gap-2">
            {isAuthenticated ? (
              <>
                <div className="d-none d-md-flex flex-column align-items-end me-2 text-end">
                  <span className="fw-semibold text-dark small">{user?.fullName || user?.username}</span>
                  <Badge bg={getRoleBadgeVariant(user?.role)} className="mt-1" style={{ fontSize: '0.65rem' }}>
                    {user?.role}
                  </Badge>
                </div>
                <Button
                  variant="primary"
                  size="sm"
                  className="fw-semibold d-flex align-items-center gap-1"
                  onClick={() => navigate(getRoleDashboardPath(user?.role))}
                >
                  <i className="bi bi-grid-fill"></i>
                  <span>Vào hệ thống</span>
                </Button>
                <Button
                  variant="outline-secondary"
                  size="sm"
                  className="fw-semibold d-flex align-items-center gap-1"
                  onClick={handleLogout}
                >
                  <i className="bi bi-box-arrow-right"></i>
                  <span className="d-none d-sm-inline">Đăng xuất</span>
                </Button>
              </>
            ) : (
              <Button
                as={Link}
                to="/login"
                variant="primary"
                size="sm"
                className="fw-semibold d-flex align-items-center gap-1 px-3"
              >
                <i className="bi bi-box-arrow-in-right"></i>
                <span>Đăng nhập</span>
              </Button>
            )}
          </div>
        </Container>
      </Navbar>

      {/* Main Page Canvas */}
      <main className="flex-grow-1 py-4">
        <Container fluid="xl" className="px-3 px-sm-4">
          {children}
        </Container>
      </main>

      {/* Footer */}
      <footer className="bg-white border-top border-light-subtle py-3 text-center text-muted small">
        <Container fluid="xl">
          <span>&copy; {new Date().getFullYear()} P05-EMS — Hệ thống Quản lý Bảo trì & Lịch bảo dưỡng Thiết bị (Đề tài P05 — Nhóm 05, FPT University).</span>
        </Container>
      </footer>
    </div>
  );
}
