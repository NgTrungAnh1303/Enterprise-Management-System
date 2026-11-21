import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Card from 'react-bootstrap/Card';
import Button from 'react-bootstrap/Button';
import Badge from 'react-bootstrap/Badge';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import { useAuth } from '../../auth/AuthContext';
import { incidentsApi } from '../../api';
import PageHeader from '../../components/common/PageHeader';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';
import LoadingState from '../../components/common/LoadingState';
import ErrorState from '../../components/common/ErrorState';

export default function IncidentListPage() {
  const { role } = useAuth();
  const navigate = useNavigate();
  const isManager = role === 'MANAGER';
  const basePath = isManager ? '/manager' : '/technician';

  const [data, setData] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [statusFilter, setStatusFilter] = useState('');
  const [severityFilter, setSeverityFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchIncidents = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = { page, size };
      if (statusFilter) params.status = statusFilter;
      if (severityFilter) params.severity = severityFilter;
      const res = await incidentsApi.getIncidents(params);
      const resData = res?.items ? res : (res?.data || {});
      setData(resData.items || []);
      setTotal(resData.total || 0);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, [page, size, statusFilter, severityFilter]);

  useEffect(() => {
    fetchIncidents();
  }, [fetchIncidents]);

  const columns = [
    {
      header: 'Mã sự cố',
      field: 'trackingCode',
      render: (row) => (
        <Link
          to={`${basePath}/incidents/${row.incidentId}`}
          className="fw-bold font-monospace text-primary text-decoration-underline"
        >
          {row.trackingCode || `INC-${row.incidentId}`}
        </Link>
      ),
    },
    {
      header: 'Thiết bị',
      field: 'equipmentCode',
      render: (row) => (
        <div>
          <span className="fw-semibold text-dark">{row.equipmentCode || '—'}</span>
          {(row.brand || row.model) && (
            <div className="text-muted small">
              {row.brand} {row.model}
            </div>
          )}
        </div>
      ),
    },
    {
      header: 'Nội dung phản ánh',
      field: 'description',
      render: (row) => (
        <div className="text-truncate" style={{ maxWidth: '280px' }} title={row.description}>
          {row.description}
        </div>
      ),
    },
    {
      header: 'Mức độ',
      field: 'severity',
      align: 'center',
      render: (row) => {
        if (!row.severity) return <Badge bg="secondary">Chưa phân loại</Badge>;
        const colorMap = {
          LOW: 'secondary',
          MEDIUM: 'info',
          HIGH: 'warning',
          CRITICAL: 'danger',
        };
        return <Badge bg={colorMap[row.severity] || 'secondary'}>{row.severity}</Badge>;
      },
    },
    {
      header: 'Trạng thái',
      field: 'status',
      align: 'center',
      render: (row) => <StatusBadge type="incident" value={row.status} />,
    },
    {
      header: 'Người báo cáo',
      field: 'reporterName',
      render: (row) => row.reporterName || '—',
    },
    {
      header: 'Ngày báo',
      field: 'createdAt',
      render: (row) => (
        <span className="font-monospace text-secondary small">
          {row.createdAt ? new Date(row.createdAt).toLocaleDateString('vi-VN') : '—'}
        </span>
      ),
    },
    {
      header: 'Thao tác',
      align: 'end',
      render: (row) => (
        <Button
          variant="outline-primary"
          size="sm"
          className="py-1 px-2"
          onClick={() => navigate(`${basePath}/incidents/${row.incidentId}`)}
        >
          <i className="bi bi-box-arrow-up-right me-1"></i>Xử lý
        </Button>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title={isManager ? 'Quản lý & Phân loại Sự cố' : 'Sự cố Kỹ thuật được Giao'}
        subtitle={
          isManager
            ? 'Thẩm định mức độ nghiêm trọng, tạo phiếu sửa chữa và theo dõi tiến độ khắc phục'
            : 'Cập nhật tiến độ xử lý và xác nhận kết quả khắc phục sự cố thiết bị'
        }
      />

      {}
      <Card className="border border-light-subtle shadow-sm mb-4">
        <Card.Body className="p-3">
          <Row className="g-3 align-items-end">
            <Col xs={12} sm={6} md={3}>
              <Form.Label className="small fw-semibold text-secondary">Trạng thái sự cố</Form.Label>
              <Form.Select
                size="sm"
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value);
                  setPage(0);
                }}
              >
                <option value="">Tất cả trạng thái</option>
                <option value="REPORTED">Mới báo cáo (REPORTED)</option>
                <option value="CLASSIFIED">Đã phân loại (CLASSIFIED)</option>
                <option value="ASSIGNED">Đã phân công (ASSIGNED)</option>
                <option value="IN_PROGRESS">Đang sửa chữa (IN_PROGRESS)</option>
                <option value="RESOLVED">Đã khắc phục (RESOLVED)</option>
                <option value="CLOSED">Đã đóng (CLOSED)</option>
              </Form.Select>
            </Col>

            <Col xs={12} sm={6} md={3}>
              <Form.Label className="small fw-semibold text-secondary">Mức độ nghiêm trọng</Form.Label>
              <Form.Select
                size="sm"
                value={severityFilter}
                onChange={(e) => {
                  setSeverityFilter(e.target.value);
                  setPage(0);
                }}
              >
                <option value="">Tất cả mức độ</option>
                <option value="LOW">Thấp (LOW)</option>
                <option value="MEDIUM">Trung bình (MEDIUM)</option>
                <option value="HIGH">Cao (HIGH)</option>
                <option value="CRITICAL">Khẩn cấp (CRITICAL)</option>
              </Form.Select>
            </Col>

            <Col xs={12} sm={6} md={3}>
              <Button
                variant="outline-secondary"
                size="sm"
                onClick={() => {
                  setStatusFilter('');
                  setSeverityFilter('');
                  setPage(0);
                }}
              >
                Đặt lại bộ lọc
              </Button>
            </Col>
          </Row>
        </Card.Body>
      </Card>

      {}
      {loading ? (
        <LoadingState message="Đang nạp danh sách sự cố..." />
      ) : error ? (
        <ErrorState
          title="Không thể tải danh sách sự cố"
          message={error.message}
          code={error.code}
          onRetry={fetchIncidents}
        />
      ) : (
        <DataTable
          columns={columns}
          data={data}
          keyField="incidentId"
          emptyTitle="Không có sự cố nào"
          emptyMessage="Không tìm thấy sự cố nào phù hợp với điều kiện lọc."
          pagination={{
            page,
            size,
            total,
            onPageChange: (newPage) => setPage(newPage),
            onSizeChange: (newSize) => {
              setSize(newSize);
              setPage(0);
            },
          }}
        />
      )}
    </div>
  );
}

