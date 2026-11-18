import React from 'react';
import { Link } from 'react-router-dom';
import Card from 'react-bootstrap/Card';
import Button from 'react-bootstrap/Button';
import { useAuth } from '../../auth/AuthContext';

export default function ForbiddenPage() {
  const { role } = useAuth();

  const getHomePath = () => {
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
        return '/login';
    }
  };

  return (
    <div className="min-vh-100 d-flex align-items-center justify-content-center bg-app p-3">
      <Card className="text-center shadow-sm border border-light-subtle p-4" style={{ maxWidth: '440px', width: '100%' }}>
        <Card.Body>
          <div
            className="rounded-circle d-inline-flex align-items-center justify-content-center mb-3"
            style={{ width: '64px', height: '64px', backgroundColor: '#FEE2E2', color: '#B91C1C' }}
          >
            <i className="bi bi-shield-x fs-2"></i>
          </div>
          <h1 className="h5 fw-bold text-dark mb-2">403 — Quyền Truy Cập Bị Từ Chối</h1>
          <p className="text-secondary small mb-4">
            Bạn không có quyền truy cập vào chức năng hoặc tài nguyên được yêu cầu. Nếu bạn cho rằng đây là sự nhầm lẫn, vui lòng liên hệ Quản trị viên.
          </p>
          <Button as={Link} to={getHomePath()} variant="primary" size="sm" className="px-4 py-2">
            Quay lại trang chủ
          </Button>
        </Card.Body>
      </Card>
    </div>
  );
}

