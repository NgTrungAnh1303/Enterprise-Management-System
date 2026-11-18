import React from 'react';
import { Link } from 'react-router-dom';
import Card from 'react-bootstrap/Card';
import Button from 'react-bootstrap/Button';
import { useAuth } from '../../auth/AuthContext';

export default function NotFoundPage() {
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
            className="rounded-circle d-inline-flex align-items-center justify-content-center mb-3 text-secondary"
            style={{ width: '64px', height: '64px', backgroundColor: '#F1F5F9' }}
          >
            <i className="bi bi-compass fs-2 text-muted"></i>
          </div>
          <h1 className="h5 fw-bold text-dark mb-2">404 — Không Tìm Thấy Trang</h1>
          <p className="text-secondary small mb-4">
            Đường dẫn bạn yêu cầu không tồn tại hoặc đã được di chuyển sang địa chỉ khác.
          </p>
          <Button as={Link} to={getHomePath()} variant="primary" size="sm" className="px-4 py-2">
            Về trang chủ
          </Button>
        </Card.Body>
      </Card>
    </div>
  );
}

