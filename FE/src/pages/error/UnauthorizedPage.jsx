import React from 'react';
import { Link } from 'react-router-dom';
import Card from 'react-bootstrap/Card';
import Button from 'react-bootstrap/Button';

export default function UnauthorizedPage() {
  return (
    <div className="min-vh-100 d-flex align-items-center justify-content-center bg-app p-3">
      <Card className="text-center shadow-sm border border-light-subtle p-4" style={{ maxWidth: '440px', width: '100%' }}>
        <Card.Body>
          <div
            className="rounded-circle d-inline-flex align-items-center justify-content-center mb-3 text-warning"
            style={{ width: '64px', height: '64px', backgroundColor: '#FFFBEB' }}
          >
            <i className="bi bi-shield-lock-fill fs-2 text-warning"></i>
          </div>
          <h1 className="h5 fw-bold text-dark mb-2">401 — Chưa Xác Thực</h1>
          <p className="text-secondary small mb-4">
            Phiên làm việc của bạn đã hết hạn hoặc chưa được xác thực. Vui lòng đăng nhập lại để tiếp tục.
          </p>
          <Button as={Link} to="/login" variant="primary" size="sm" className="px-4 py-2">
            Đăng nhập lại
          </Button>
        </Card.Body>
      </Card>
    </div>
  );
}

