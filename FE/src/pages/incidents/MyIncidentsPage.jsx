import React, { useState, useEffect, useCallback } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import Card from 'react-bootstrap/Card';
import Button from 'react-bootstrap/Button';
import Badge from 'react-bootstrap/Badge';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Modal from 'react-bootstrap/Modal';
import Alert from 'react-bootstrap/Alert';
import { incidentsApi } from '../../api';
import PageHeader from '../../components/common/PageHeader';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';
import LoadingState from '../../components/common/LoadingState';
import ErrorState from '../../components/common/ErrorState';

export default function MyIncidentsPage() {
  const { id: paramId } = useParams();
  const navigate = useNavigate();

  const [data, setData] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  
  const [selectedIncident, setSelectedIncident] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState(null);
  const [showModal, setShowModal] = useState(false);

  
  const fetchIncidents = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = { page, size };
      if (statusFilter) params.status = statusFilter;
      const res = await incidentsApi.getIncidents(params);
      const resData = res?.items ? res : (res?.data || {});
      setData(resData.items || []);
      setTotal(resData.total || 0);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, [page, size, statusFilter]);

  useEffect(() => {
    fetchIncidents();
  }, [fetchIncidents]);

  
  const loadSingleIncident = useCallback(async (id) => {
    setDetailLoading(true);
    setDetailError(null);
    setShowModal(true);
    try {
      const res = await incidentsApi.getIncidentById(id);
      setSelectedIncident(res?.data || res);
    } catch (err) {
      setDetailError(err);
      setSelectedIncident(null);
    } finally {
      setDetailLoading(false);
    }
  }, []);

  useEffect(() => {
    if (paramId) {
      loadSingleIncident(paramId);
    }
  }, [paramId, loadSingleIncident]);

  const handleCloseModal = () => {
    setShowModal(false);
    setSelectedIncident(null);
    setDetailError(null);
    if (paramId) {
      navigate('/user/incidents');
    }
  };

  const columns = [
    {
      header: 'Mã theo dõi',
      field: 'trackingCode',
      render: (row) => (
        <button
          type="button"
          className="btn btn-link p-0 fw-bold font-monospace text-primary text-decoration-underline text-start"
          onClick={() => loadSingleIncident(row.incidentId)}
        >
          {row.trackingCode || `INC-${row.incidentId}`}
        </button>
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
      header: 'Mô tả hiện tượng',
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
        if (!row.severity) return <span className="text-muted small fst-italic">Chưa phân loại</span>;
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
      header: 'Thời điểm báo',
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
          onClick={() => loadSingleIncident(row.incidentId)}
        >
          <i className="bi bi-eye me-1"></i>Chi tiết
        </Button>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Sự cố của tôi"
        subtitle="Theo dõi tiến độ xử lý và kết quả khắc phục các sự cố thiết bị do bạn báo cáo"
        actions={
          <Button as={Link} to="/user/incidents/new" variant="primary" size="sm" className="d-flex align-items-center gap-1">
            <i className="bi bi-plus-circle"></i>
            <span>Báo sự cố mới</span>
          </Button>
        }
      />

      {}
      <Card className="border border-light-subtle shadow-sm mb-4">
        <Card.Body className="p-3">
          <Row className="g-3 align-items-end">
            <Col xs={12} sm={6} md={4}>
              <Form.Label className="small fw-semibold text-secondary">Lọc theo trạng thái sự cố</Form.Label>
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
                <option value="ASSIGNED">Đã phân công sửa chữa (ASSIGNED)</option>
                <option value="IN_PROGRESS">Đang sửa chữa (IN_PROGRESS)</option>
                <option value="RESOLVED">Đã khắc phục (RESOLVED)</option>
                <option value="CLOSED">Đã đóng sự cố (CLOSED)</option>
              </Form.Select>
            </Col>
            <Col xs={12} sm={6} md={3}>
              <Button
                variant="outline-secondary"
                size="sm"
                onClick={() => {
                  setStatusFilter('');
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
        <LoadingState message="Đang nạp danh sách sự cố của bạn..." />
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
          emptyTitle="Chưa có sự cố nào"
          emptyMessage="Bạn chưa gửi báo cáo sự cố nào hoặc không có sự cố phù hợp với bộ lọc."
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

      {}
      <Modal show={showModal} onHide={handleCloseModal} size="lg" centered>
        <Modal.Header closeButton className="bg-light border-bottom">
          <Modal.Title className="h6 fw-bold">
            <i className="bi bi-info-circle me-2 text-primary"></i>
            Chi tiết Báo cáo Sự cố
          </Modal.Title>
        </Modal.Header>
        <Modal.Body className="p-4">
          {detailLoading ? (
            <LoadingState message="Đang tải thông tin chi tiết sự cố..." />
          ) : detailError ? (
            <Alert variant="danger" className="mb-0 border border-danger">
              <div className="d-flex align-items-center gap-2 mb-2">
                <i className="bi bi-shield-x fs-4 text-danger"></i>
                <h6 className="fw-bold mb-0">Truy cập bị từ chối (403 Forbidden)</h6>
              </div>
              <p className="mb-1 small">
                {detailError.status === 403 || detailError.code === 'INCIDENT_ACCESS_DENIED'
                  ? 'Bạn không có quyền truy cập sự cố này vì bạn không phải người báo cáo hoặc không được giao quyền quản trị.'
                  : detailError.message || 'Không thể xem chi tiết sự cố.'}
              </p>
              <div className="font-monospace small text-muted">Mã lỗi: {detailError.code || 'ACCESS_DENIED'}</div>
            </Alert>
          ) : selectedIncident ? (
            <div>
              <div className="d-flex justify-content-between align-items-center border-bottom pb-3 mb-3">
                <div>
                  <div className="text-secondary small">Mã theo dõi</div>
                  <div className="font-monospace fw-bold text-primary fs-5">
                    {selectedIncident.trackingCode || `INC-${selectedIncident.incidentId}`}
                  </div>
                </div>
                <div className="text-end">
                  <div className="text-secondary small mb-1">Trạng thái</div>
                  <StatusBadge type="incident" value={selectedIncident.status} />
                </div>
              </div>

              <Row className="g-3 mb-3">
                <Col md={6}>
                  <div className="text-muted small">Thiết bị gặp sự cố</div>
                  <div className="fw-bold text-dark">
                    {selectedIncident.equipmentCode} — {selectedIncident.brand} {selectedIncident.model}
                  </div>
                </Col>
                <Col md={6}>
                  <div className="text-muted small">Mức độ nghiêm trọng</div>
                  <div>
                    {selectedIncident.severity ? (
                      <Badge bg={selectedIncident.severity === 'CRITICAL' ? 'danger' : 'warning'}>
                        {selectedIncident.severity}
                      </Badge>
                    ) : (
                      <span className="text-muted small fst-italic">Đang chờ Ban quản lý thẩm định</span>
                    )}
                  </div>
                </Col>
                <Col md={6}>
                  <div className="text-muted small">Người báo cáo</div>
                  <div className="fw-semibold text-dark">{selectedIncident.reporterName || 'Chính bạn'}</div>
                </Col>
                <Col md={6}>
                  <div className="text-muted small">Thời điểm báo cáo</div>
                  <div className="font-monospace small">
                    {selectedIncident.createdAt ? new Date(selectedIncident.createdAt).toLocaleString('vi-VN') : '—'}
                  </div>
                </Col>
              </Row>

              <div className="mb-3">
                <div className="text-muted small mb-1">Nội dung phản ánh hư hỏng:</div>
                <div className="p-3 bg-light rounded border text-dark font-monospace small">
                  {selectedIncident.description}
                </div>
              </div>

              {selectedIncident.closedAt && (
                <Alert variant="success" className="small mb-0">
                  <i className="bi bi-check-all me-2"></i>
                  Sự cố đã được hoàn tất khắc phục và nghiệm thu đóng vào ngày{' '}
                  {new Date(selectedIncident.closedAt).toLocaleString('vi-VN')}.
                </Alert>
              )}
            </div>
          ) : null}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" size="sm" onClick={handleCloseModal}>
            Đóng
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
}

