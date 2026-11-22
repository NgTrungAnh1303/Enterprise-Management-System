import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Card from 'react-bootstrap/Card';
import Button from 'react-bootstrap/Button';
import { useAuth } from '../../auth/AuthContext';
import { incidentsApi } from '../../api';
import PageHeader from '../../components/common/PageHeader';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';

export default function UserDashboard() {
  const { user } = useAuth();
  const [incidents, setIncidents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchMyIncidents = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await incidentsApi.getIncidents({ page: 0, size: 5 });
      setIncidents(res.items || []);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMyIncidents();
  }, []);

  const columns = [
    {
      header: 'Mã tra cứu',
      key: 'trackingCode',
      render: (row) => (
        <span className="font-monospace-code fw-semibold text-primary">
          {row.trackingCode || row.code || `INC-${row.incidentId}`}
        </span>
      ),
    },
    {
      header: 'Thiết bị',
      key: 'equipmentCode',
      render: (row) => <span className="font-monospace-code fw-medium">{row.equipmentCode || row.equipment?.code || '—'}</span>,
    },
    {
      header: 'Mô tả sự cố',
      key: 'description',
      render: (row) => (
        <span className="text-secondary small text-truncate d-inline-block" style={{ maxWidth: '300px' }}>
          {row.description || '—'}
        </span>
      ),
    },
    {
      header: 'Mức độ',
      key: 'severity',
      cellClassName: 'text-center',
      headerClassName: 'text-center',
      render: (row) => (row.severity ? <StatusBadge type="severity" status={row.severity} /> : <span className="text-muted small">Chưa phân loại</span>),
    },
    {
      header: 'Trạng thái',
      key: 'status',
      cellClassName: 'text-center',
      headerClassName: 'text-center',
      render: (row) => <StatusBadge type="incident" status={row.status} />,
    },
  ];

  return (
    <div>
      <PageHeader
        title={`Xin chào, ${user?.fullName || user?.username}!`}
        subtitle="Bàn làm việc Người dùng thiết bị — Tra cứu thiết bị và quản lý các sự cố đã báo"
        actions={
          <Button as={Link} to="/user/incidents/new" variant="primary" size="sm" className="d-flex align-items-center gap-1">
            <i className="bi bi-exclamation-triangle"></i>
            <span>Báo sự cố thiết bị mới</span>
          </Button>
        }
      />

      {/* Quick Summary Cards */}
      <Row className="g-3 mb-4">
        <Col sm={6} lg={4}>
          <Card className="border border-light-subtle shadow-sm h-100">
            <Card.Body className="p-3 d-flex align-items-center gap-3">
              <div
                className="rounded-3 d-flex align-items-center justify-content-center text-primary"
                style={{ width: '48px', height: '48px', backgroundColor: '#EFF6FF' }}
              >
                <i className="bi bi-cpu fs-4"></i>
              </div>
              <div>
                <div className="text-secondary small">Tra cứu thiết bị</div>
                <Link to="/user/equipment" className="fw-bold text-dark text-decoration-none small stretched-link">
                  Tìm kiếm thiết bị phòng lab &rarr;
                </Link>
              </div>
            </Card.Body>
          </Card>
        </Col>

        <Col sm={6} lg={4}>
          <Card className="border border-light-subtle shadow-sm h-100">
            <Card.Body className="p-3 d-flex align-items-center gap-3">
              <div
                className="rounded-3 d-flex align-items-center justify-content-center text-warning"
                style={{ width: '48px', height: '48px', backgroundColor: '#FFFBEB' }}
              >
                <i className="bi bi-exclamation-triangle fs-4 text-warning"></i>
              </div>
              <div>
                <div className="text-secondary small">Sự cố thiết bị</div>
                <Link to="/user/incidents/new" className="fw-bold text-dark text-decoration-none small stretched-link">
                  Gửi yêu cầu báo sự cố mới &rarr;
                </Link>
              </div>
            </Card.Body>
          </Card>
        </Col>

        <Col sm={6} lg={4}>
          <Card className="border border-light-subtle shadow-sm h-100">
            <Card.Body className="p-3 d-flex align-items-center gap-3">
              <div
                className="rounded-3 d-flex align-items-center justify-content-center text-success"
                style={{ width: '48px', height: '48px', backgroundColor: '#F0FDF4' }}
              >
                <i className="bi bi-card-checklist fs-4 text-success"></i>
              </div>
              <div>
                <div className="text-secondary small">Theo dõi tiến độ</div>
                <Link to="/user/incidents" className="fw-bold text-dark text-decoration-none small stretched-link">
                  Xem toàn bộ sự cố của tôi &rarr;
                </Link>
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Recent Incidents Table */}
      <div className="mb-2 d-flex align-items-center justify-content-between">
        <h2 className="h6 fw-bold text-dark mb-0">Sự cố kỹ thuật đã báo gần đây</h2>
        <Link to="/user/incidents" className="small text-decoration-none text-primary fw-medium">
          Xem tất cả &rarr;
        </Link>
      </div>

      <DataTable
        columns={columns}
        data={incidents}
        loading={loading}
        error={error}
        emptyTitle="Chưa có sự cố nào"
        emptyMessage="Bạn chưa gửi báo cáo sự cố nào. Nếu thiết bị gặp trục trặc, hãy bấm nút 'Báo sự cố thiết bị mới'."
        onRetry={fetchMyIncidents}
        keyField="incidentId"
      />
    </div>
  );
}

