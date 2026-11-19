import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Card from 'react-bootstrap/Card';
import { useAuth } from '../../auth/AuthContext';
import { workOrdersApi } from '../../api';
import PageHeader from '../../components/common/PageHeader';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';

export default function TechnicianDashboard() {
  const { user } = useAuth();
  const [workOrders, setWorkOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchAssignedWorkOrders = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await workOrdersApi.getWorkOrders({ page: 0, size: 5 });
      setWorkOrders(res.items || []);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAssignedWorkOrders();
  }, []);

  const inProgressCount = workOrders.filter((w) => w.status === 'IN_PROGRESS').length;
  const assignedCount = workOrders.filter((w) => w.status === 'ASSIGNED').length;

  const columns = [
    {
      header: 'Mã Phiếu (WO)',
      key: 'woCode',
      render: (val, row) => (
        <span className="font-monospace-code fw-semibold text-primary">
          {val || `WO-${row.woId}`}
        </span>
      ),
    },
    {
      header: 'Thiết bị',
      key: 'equipmentCode',
      render: (val) => <span className="font-monospace-code fw-medium">{val || '—'}</span>,
    },
    {
      header: 'Loại bảo trì',
      key: 'type',
      render: (val) => (
        <span className="badge bg-light text-dark border small">
          {val === 'PERIODIC' ? 'Định kỳ' : 'Sửa chữa'}
        </span>
      ),
    },
    {
      header: 'Kỳ bảo trì',
      key: 'period',
      render: (val) => <span className="text-secondary small">{val || '—'}</span>,
    },
    {
      header: 'Trạng thái',
      key: 'status',
      cellClassName: 'text-center',
      headerClassName: 'text-center',
      render: (val) => <StatusBadge type="workOrder" status={val} />,
    },
    {
      header: 'Thao tác',
      key: 'actions',
      cellClassName: 'text-end',
      headerClassName: 'text-end',
      render: (_, row) => (
        <Link
          to={`/technician/work-orders/${row.woId || row.id}`}
          className="btn btn-sm btn-outline-primary py-0 px-2"
        >
          {row.status === 'IN_PROGRESS' ? 'Tiếp tục' : 'Chi tiết'} &rarr;
        </Link>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title={`Bàn làm việc Kỹ thuật — ${user?.fullName || user?.username}`}
        subtitle="Quản lý và thực hiện các phiếu bảo trì, xử lý sự cố thiết bị được phân công"
      />

      {}
      <Row className="g-3 mb-4">
        <Col sm={6} lg={4}>
          <Card className="border border-light-subtle shadow-sm h-100">
            <Card.Body className="p-3 d-flex align-items-center gap-3">
              <div
                className="rounded-3 d-flex align-items-center justify-content-center text-warning"
                style={{ width: '48px', height: '48px', backgroundColor: '#FEF3C7' }}
              >
                <i className="bi bi-play-circle fs-4 text-warning"></i>
              </div>
              <div>
                <div className="text-secondary small">WO Đang thực hiện</div>
                <div className="h4 fw-bold mb-0 text-dark">{inProgressCount}</div>
              </div>
            </Card.Body>
          </Card>
        </Col>

        <Col sm={6} lg={4}>
          <Card className="border border-light-subtle shadow-sm h-100">
            <Card.Body className="p-3 d-flex align-items-center gap-3">
              <div
                className="rounded-3 d-flex align-items-center justify-content-center text-primary"
                style={{ width: '48px', height: '48px', backgroundColor: '#EDE9FE' }}
              >
                <i className="bi bi-person-check fs-4" style={{ color: '#6D28D9' }}></i>
              </div>
              <div>
                <div className="text-secondary small">WO Chờ bắt đầu</div>
                <div className="h4 fw-bold mb-0 text-dark">{assignedCount}</div>
              </div>
            </Card.Body>
          </Card>
        </Col>

        <Col sm={6} lg={4}>
          <Card className="border border-light-subtle shadow-sm h-100">
            <Card.Body className="p-3 d-flex align-items-center justify-content-between">
              <div className="d-flex align-items-center gap-3">
                <div
                  className="rounded-3 d-flex align-items-center justify-content-center text-success"
                  style={{ width: '48px', height: '48px', backgroundColor: '#F0FDF4' }}
                >
                  <i className="bi bi-tools fs-4 text-success"></i>
                </div>
                <div>
                  <div className="text-secondary small">Tất cả phiếu được giao</div>
                  <div className="h4 fw-bold mb-0 text-dark">{workOrders.length}</div>
                </div>
              </div>
              <Link to="/technician/work-orders" className="btn btn-sm btn-outline-success d-flex align-items-center gap-1">
                <span>Xem danh sách</span>
                <i className="bi bi-arrow-right"></i>
              </Link>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {}
      <div className="mb-2 d-flex align-items-center justify-content-between">
        <h2 className="h6 fw-bold text-dark mb-0">Phiếu bảo trì cần xử lý ngay</h2>
        <Link to="/technician/work-orders" className="small text-decoration-none text-primary fw-medium">
          Xem tất cả ({workOrders.length}) &rarr;
        </Link>
      </div>

      <DataTable
        columns={columns}
        data={workOrders}
        loading={loading}
        error={error}
        emptyTitle="Không có phiếu bảo trì nào đang chờ"
        emptyMessage="Hiện tại bạn chưa được phân công phiếu bảo trì hoặc sự cố nào. Hãy kiểm tra lại sau."
        onRetry={fetchAssignedWorkOrders}
        keyField="woId"
      />
    </div>
  );
}

