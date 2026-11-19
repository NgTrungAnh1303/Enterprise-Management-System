import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Card from 'react-bootstrap/Card';
import { usersApi, equipmentApi, equipmentTypesApi, locationsApi } from '../../api';
import PageHeader from '../../components/common/PageHeader';
import LoadingState from '../../components/common/LoadingState';
import ErrorState from '../../components/common/ErrorState';

export default function AdminDashboard() {
  const [counts, setCounts] = useState({
    users: 0,
    equipment: 0,
    types: 0,
    locations: 0,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchAdminData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [usersRes, eqRes, typesRes, locsRes] = await Promise.all([
        usersApi.getUsers({ page: 0, size: 1 }).catch(() => ({ total: 0 })),
        equipmentApi.getEquipment({ page: 0, size: 1 }).catch(() => ({ total: 0 })),
        equipmentTypesApi.getEquipmentTypes().catch(() => []),
        locationsApi.getLocations().catch(() => []),
      ]);

      setCounts({
        users: usersRes.total || 0,
        equipment: eqRes.total || 0,
        types: Array.isArray(typesRes) ? typesRes.length : 0,
        locations: Array.isArray(locsRes) ? locsRes.length : 0,
      });
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAdminData();
  }, []);

  if (loading) {
    return <LoadingState message="Đang nạp dữ liệu quản trị hệ thống..." />;
  }

  if (error) {
    return (
      <ErrorState
        title="Lỗi tải dữ liệu bảng điều khiển"
        message={error.message}
        code={error.code}
        requestId={error.requestId}
        onRetry={fetchAdminData}
      />
    );
  }

  return (
    <div>
      <PageHeader
        title="Bảng Điều Khiển Quản Trị Hệ Thống"
        subtitle="Quản lý tài khoản, danh mục kỹ thuật và giám sát nhật ký kiểm toán hệ thống"
      />

      {/* Admin Metrics Cards (Only data Admin is permitted to read) */}
      <Row className="g-3 mb-4">
        <Col sm={6} lg={3}>
          <Card className="border border-light-subtle shadow-sm h-100">
            <Card.Body className="p-3 d-flex align-items-center gap-3">
              <div
                className="rounded-3 d-flex align-items-center justify-content-center text-primary"
                style={{ width: '48px', height: '48px', backgroundColor: '#DBEAFE' }}
              >
                <i className="bi bi-people-fill fs-4 text-primary"></i>
              </div>
              <div>
                <div className="text-secondary small fw-medium">Tài khoản người dùng</div>
                <div className="h3 fw-bold text-dark mt-1 mb-0">{counts.users}</div>
              </div>
            </Card.Body>
          </Card>
        </Col>

        <Col sm={6} lg={3}>
          <Card className="border border-light-subtle shadow-sm h-100">
            <Card.Body className="p-3 d-flex align-items-center gap-3">
              <div
                className="rounded-3 d-flex align-items-center justify-content-center text-success"
                style={{ width: '48px', height: '48px', backgroundColor: '#DCFCE7' }}
              >
                <i className="bi bi-cpu-fill fs-4 text-success"></i>
              </div>
              <div>
                <div className="text-secondary small fw-medium">Tổng số thiết bị</div>
                <div className="h3 fw-bold text-dark mt-1 mb-0">{counts.equipment}</div>
              </div>
            </Card.Body>
          </Card>
        </Col>

        <Col sm={6} lg={3}>
          <Card className="border border-light-subtle shadow-sm h-100">
            <Card.Body className="p-3 d-flex align-items-center gap-3">
              <div
                className="rounded-3 d-flex align-items-center justify-content-center"
                style={{ width: '48px', height: '48px', backgroundColor: '#EDE9FE', color: '#6D28D9' }}
              >
                <i className="bi bi-tags-fill fs-4"></i>
              </div>
              <div>
                <div className="text-secondary small fw-medium">Loại thiết bị</div>
                <div className="h3 fw-bold text-dark mt-1 mb-0">{counts.types}</div>
              </div>
            </Card.Body>
          </Card>
        </Col>

        <Col sm={6} lg={3}>
          <Card className="border border-light-subtle shadow-sm h-100">
            <Card.Body className="p-3 d-flex align-items-center gap-3">
              <div
                className="rounded-3 d-flex align-items-center justify-content-center text-warning"
                style={{ width: '48px', height: '48px', backgroundColor: '#FEF3C7' }}
              >
                <i className="bi bi-geo-alt-fill fs-4 text-warning"></i>
              </div>
              <div>
                <div className="text-secondary small fw-medium">Vị trí lắp đặt</div>
                <div className="h3 fw-bold text-dark mt-1 mb-0">{counts.locations}</div>
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Admin Shortcuts Grid */}
      <h2 className="h6 fw-bold text-dark mb-3">Lối tắt chức năng Quản trị</h2>
      <Row className="g-3">
        <Col md={6} xl={4}>
          <Card className="border border-light-subtle shadow-sm h-100">
            <Card.Body className="p-4">
              <div className="d-flex align-items-center gap-3 mb-3">
                <div className="p-2 bg-primary bg-opacity-10 text-primary rounded">
                  <i className="bi bi-person-gear fs-4"></i>
                </div>
                <h3 className="h6 fw-bold mb-0 text-dark">Quản lý Tài khoản & Phân quyền</h3>
              </div>
              <p className="text-secondary small mb-3">
                Thêm mới người dùng, cấp vai trò (EQUIPMENT_USER, TECHNICIAN, MANAGER, ADMIN), khóa và kích hoạt tài khoản.
              </p>
              <Link to="/admin/users" className="btn btn-outline-primary btn-sm d-inline-flex align-items-center gap-1">
                <span>Vào trang quản lý</span>
                <i className="bi bi-arrow-right"></i>
              </Link>
            </Card.Body>
          </Card>
        </Col>

        <Col md={6} xl={4}>
          <Card className="border border-light-subtle shadow-sm h-100">
            <Card.Body className="p-4">
              <div className="d-flex align-items-center gap-3 mb-3">
                <div className="p-2 bg-success bg-opacity-10 text-success rounded">
                  <i className="bi bi-hdd-rack fs-4"></i>
                </div>
                <h3 className="h6 fw-bold mb-0 text-dark">Danh mục Thiết bị & Vị trí</h3>
              </div>
              <p className="text-secondary small mb-3">
                Tạo mới, chỉnh sửa thông tin thiết bị, phân nhóm loại thiết bị và chuẩn hóa phòng lab, khu vực lắp đặt.
              </p>
              <div className="d-flex gap-2">
                <Link to="/admin/equipment-catalog" className="btn btn-outline-success btn-sm">
                  Thiết bị
                </Link>
                <Link to="/admin/equipment-types" className="btn btn-outline-success btn-sm">
                  Loại thiết bị
                </Link>
                <Link to="/admin/locations" className="btn btn-outline-success btn-sm">
                  Vị trí
                </Link>
              </div>
            </Card.Body>
          </Card>
        </Col>

        <Col md={6} xl={4}>
          <Card className="border border-light-subtle shadow-sm h-100">
            <Card.Body className="p-4">
              <div className="d-flex align-items-center gap-3 mb-3">
                <div className="p-2 bg-danger bg-opacity-10 text-danger rounded">
                  <i className="bi bi-journal-text fs-4"></i>
                </div>
                <h3 className="h6 fw-bold mb-0 text-dark">Nhật ký Hệ thống (Audit Log)</h3>
              </div>
              <p className="text-secondary small mb-3">
                Tra cứu toàn diện các thao tác đăng nhập, cảnh báo bảo mật, quyền truy cập bị từ chối và thay đổi dữ liệu.
              </p>
              <Link to="/admin/audit-logs" className="btn btn-outline-danger btn-sm d-inline-flex align-items-center gap-1">
                <span>Xem nhật ký kiểm toán</span>
                <i className="bi bi-arrow-right"></i>
              </Link>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </div>
  );
}

