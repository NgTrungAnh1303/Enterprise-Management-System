import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Card from 'react-bootstrap/Card';
import Button from 'react-bootstrap/Button';
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js';
import { Doughnut } from 'react-chartjs-2';
import { dashboardApi } from '../../api';
import PageHeader from '../../components/common/PageHeader';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';
import LoadingState from '../../components/common/LoadingState';
import ErrorState from '../../components/common/ErrorState';

ChartJS.register(ArcElement, Tooltip, Legend);

export default function ManagerDashboard() {
  const [dashboardData, setDashboardData] = useState(null);
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [dashRes, alertsRes] = await Promise.all([
        dashboardApi.getDashboard(),
        dashboardApi.getAlerts(),
      ]);
      setDashboardData(dashRes);
      setAlerts(alertsRes || []);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  if (loading) {
    return <LoadingState message="Đang tải dữ liệu bảng điều khiển quản lý..." />;
  }

  if (error) {
    return (
      <ErrorState
        title="Không thể tải dữ liệu Dashboard"
        message={error.message}
        code={error.code}
        requestId={error.requestId}
        onRetry={fetchData}
      />
    );
  }

  const eqByStatus = dashboardData?.equipmentByStatus || {};
  const activeCount = eqByStatus.ACTIVE || 0;
  const underMaintenanceCount = eqByStatus.UNDER_MAINTENANCE || 0;
  const outOfServiceCount = eqByStatus.OUT_OF_SERVICE || 0;
  const totalEquipment = activeCount + underMaintenanceCount + outOfServiceCount;

  const chartData = {
    labels: ['Hoạt động (ACTIVE)', 'Đang bảo dưỡng', 'Ngừng hoạt động (OOS)'],
    datasets: [
      {
        data: [activeCount, underMaintenanceCount, outOfServiceCount],
        backgroundColor: ['#15803D', '#B45309', '#B91C1C'],
        borderWidth: 1,
      },
    ],
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: { boxWidth: 12, font: { size: 11 } },
      },
    },
  };

  const alertColumns = [
    {
      header: 'Mã thiết bị',
      key: 'equipmentCode',
      render: (row) => (
        <span className="font-monospace-code fw-semibold text-primary">
          {row.equipmentCode || row.code || `EQ-${row.equipmentId}`}
        </span>
      ),
    },
    {
      header: 'Tên / Model',
      key: 'model',
      render: (row) => `${row.brand || ''} ${row.model || ''}`.trim() || '—',
    },
    {
      header: 'Ngày đến hạn bảo trì',
      key: 'nextDueDate',
      render: (row) => (
        <span className="font-monospace-code small">
          {row.nextDueDate ? String(row.nextDueDate) : 'Chưa lập kế hoạch'}
        </span>
      ),
    },
    {
      header: 'Tình trạng hạn',
      key: 'dueLabel',
      cellClassName: 'text-center',
      headerClassName: 'text-center',
      render: (row) => <StatusBadge type="due" status={row.dueLabel || row.label} />,
    },
    {
      header: 'Thao tác',
      key: 'actions',
      cellClassName: 'text-end',
      headerClassName: 'text-end',
      render: (row) => (
        <Button
          as={Link}
          to={`/manager/work-orders/new?equipmentId=${row.equipmentId}`}
          variant="outline-primary"
          size="sm"
          className="py-0 px-2"
        >
          <i className="bi bi-plus-lg me-1"></i>Tạo WO
        </Button>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Dashboard Quản lý & Cảnh báo"
        subtitle="Tổng quan tình trạng thiết bị phòng lab, lịch bảo trì định kỳ và sự cố phát sinh"
        actions={
          <Button as={Link} to="/manager/work-orders/new" variant="primary" size="sm" className="d-flex align-items-center gap-1">
            <i className="bi bi-plus-circle"></i>
            <span>Tạo Work Order mới</span>
          </Button>
        }
      />

      {/* KPI Cards Row */}
      <Row className="g-3 mb-4">
        <Col sm={6} xl={2}>
          <Card className="border border-light-subtle shadow-sm h-100">
            <Card.Body className="p-3">
              <div className="text-secondary small fw-medium">Tổng thiết bị</div>
              <div className="h3 fw-bold text-dark mt-1 mb-0">{totalEquipment}</div>
              <small className="text-muted" style={{ fontSize: '0.7rem' }}>Toàn đơn vị</small>
            </Card.Body>
          </Card>
        </Col>

        <Col sm={6} xl={2}>
          <Card className="border border-light-subtle shadow-sm h-100" style={{ borderLeft: '4px solid #15803D' }}>
            <Card.Body className="p-3">
              <div className="text-success small fw-semibold">Sẵn sàng (Active)</div>
              <div className="h3 fw-bold text-dark mt-1 mb-0">{activeCount}</div>
              <small className="text-muted" style={{ fontSize: '0.7rem' }}>Vận hành tốt</small>
            </Card.Body>
          </Card>
        </Col>

        <Col sm={6} xl={2}>
          <Card className="border border-light-subtle shadow-sm h-100" style={{ borderLeft: '4px solid #B45309' }}>
            <Card.Body className="p-3">
              <div className="text-warning small fw-semibold">Đang bảo dưỡng</div>
              <div className="h3 fw-bold text-dark mt-1 mb-0">{underMaintenanceCount}</div>
              <small className="text-muted" style={{ fontSize: '0.7rem' }}>Đang bảo trì/sửa</small>
            </Card.Body>
          </Card>
        </Col>

        <Col sm={6} xl={2}>
          <Card className="border border-light-subtle shadow-sm h-100" style={{ borderLeft: '4px solid #B91C1C' }}>
            <Card.Body className="p-3">
              <div className="text-danger small fw-semibold">Ngừng hoạt động</div>
              <div className="h3 fw-bold text-dark mt-1 mb-0">{outOfServiceCount}</div>
              <small className="text-muted" style={{ fontSize: '0.7rem' }}>Hỏng hóc / OOS</small>
            </Card.Body>
          </Card>
        </Col>

        <Col sm={6} xl={2}>
          <Card className="border border-light-subtle shadow-sm h-100" style={{ backgroundColor: '#FFF1F2' }}>
            <Card.Body className="p-3">
              <div className="text-danger small fw-semibold">Bảo trì quá hạn</div>
              <div className="h3 fw-bold text-danger mt-1 mb-0">{dashboardData?.overdueCount || 0}</div>
              <small className="text-danger" style={{ fontSize: '0.7rem' }}>Cần xử lý ngay</small>
            </Card.Body>
          </Card>
        </Col>

        <Col sm={6} xl={2}>
          <Card className="border border-light-subtle shadow-sm h-100" style={{ backgroundColor: '#FFFBEB' }}>
            <Card.Body className="p-3">
              <div className="text-warning small fw-semibold">Đến hạn (7 ngày)</div>
              <div className="h3 fw-bold text-warning mt-1 mb-0">{dashboardData?.dueSoonCount || 0}</div>
              <small className="text-muted" style={{ fontSize: '0.7rem' }}>Cần lập phiếu</small>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Visual Chart & Alerts Row */}
      <Row className="g-4 mb-4">
        <Col lg={4}>
          <Card className="border border-light-subtle shadow-sm h-100">
            <Card.Header className="bg-white py-3 border-bottom border-light-subtle">
              <h2 className="h6 fw-bold mb-0 text-dark">Phân bố trạng thái thiết bị</h2>
            </Card.Header>
            <Card.Body className="p-3">
              <div style={{ height: '180px' }} className="d-flex align-items-center justify-content-center mb-3">
                <Doughnut data={chartData} options={chartOptions} />
              </div>
              <table className="table table-sm table-bordered text-center small mb-0">
                <thead className="table-light">
                  <tr>
                    <th className="text-start">Trạng thái</th>
                    <th>Số lượng</th>
                    <th>Tỷ lệ</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td className="text-start"><span className="badge bg-success me-1">●</span> Sẵn sàng (ACTIVE)</td>
                    <td className="fw-bold">{activeCount}</td>
                    <td>{totalEquipment > 0 ? ((activeCount / totalEquipment) * 100).toFixed(1) : 0}%</td>
                  </tr>
                  <tr>
                    <td className="text-start"><span className="badge bg-warning text-dark me-1">●</span> Đang bảo dưỡng</td>
                    <td className="fw-bold">{underMaintenanceCount}</td>
                    <td>{totalEquipment > 0 ? ((underMaintenanceCount / totalEquipment) * 100).toFixed(1) : 0}%</td>
                  </tr>
                  <tr>
                    <td className="text-start"><span className="badge bg-danger me-1">●</span> Ngừng hoạt động (OOS)</td>
                    <td className="fw-bold">{outOfServiceCount}</td>
                    <td>{totalEquipment > 0 ? ((outOfServiceCount / totalEquipment) * 100).toFixed(1) : 0}%</td>
                  </tr>
                </tbody>
              </table>
            </Card.Body>
          </Card>
        </Col>

        <Col lg={8}>
          <Card className="border border-light-subtle shadow-sm h-100">
            <Card.Header className="bg-white py-3 border-bottom border-light-subtle d-flex align-items-center justify-content-between">
              <h2 className="h6 fw-bold mb-0 text-dark d-flex align-items-center gap-2">
                <i className="bi bi-bell-fill text-warning"></i>
                <span>Cảnh báo thiết bị Đến hạn & Quá hạn bảo trì</span>
              </h2>
              <span className="badge bg-danger">{alerts.length} thiết bị</span>
            </Card.Header>
            <Card.Body className="p-0">
              <DataTable
                columns={alertColumns}
                data={alerts}
                emptyTitle="Không có cảnh báo hạn bảo trì"
                emptyMessage="Hiện tại toàn bộ thiết bị đều an toàn trong kỳ hạn bảo dưỡng định kỳ."
                keyField="equipmentId"
              />
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </div>
  );
}

