import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import Card from 'react-bootstrap/Card';
import Button from 'react-bootstrap/Button';
import Badge from 'react-bootstrap/Badge';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Form from 'react-bootstrap/Form';
import Alert from 'react-bootstrap/Alert';
import Spinner from 'react-bootstrap/Spinner';
import { useAuth } from '../../auth/AuthContext';
import { incidentsApi } from '../../api';
import PageHeader from '../../components/common/PageHeader';
import StatusBadge from '../../components/common/StatusBadge';
import LoadingState from '../../components/common/LoadingState';
import ErrorState from '../../components/common/ErrorState';

export default function IncidentDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { role } = useAuth();
  const isManager = role === 'MANAGER';
  const isTechnician = role === 'TECHNICIAN';

  const [incident, setIncident] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  
  const [selectedSeverity, setSelectedSeverity] = useState('MEDIUM');
  const [isClassifying, setIsClassifying] = useState(false);
  const [classifyError, setClassifyError] = useState(null);
  const [classifySuccess, setClassifySuccess] = useState(null);

  
  const [progressStatus, setProgressStatus] = useState('RESOLVED');
  const [progressNote, setProgressNote] = useState('');
  const [isProgressing, setIsProgressing] = useState(false);
  const [progressError, setProgressError] = useState(null);
  const [progressSuccess, setProgressSuccess] = useState(null);

  
  const [isClosing, setIsClosing] = useState(false);
  const [closeError, setCloseError] = useState(null);
  const [closeSuccess, setCloseSuccess] = useState(null);

  const fetchIncident = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await incidentsApi.getIncidentById(id);
      const incData = res?.data || res;
      setIncident(incData);
      if (incData.severity) {
        setSelectedSeverity(incData.severity);
      }
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchIncident();
  }, [fetchIncident]);

  
  const handleClassify = async (e) => {
    e.preventDefault();
    setIsClassifying(true);
    setClassifyError(null);
    setClassifySuccess(null);
    try {
      await incidentsApi.classifyIncident(id, { severity: selectedSeverity });
      setClassifySuccess('Phân loại mức độ nghiêm trọng thành công!');
      fetchIncident();
    } catch (err) {
      setClassifyError(err.message || 'Không thể phân loại sự cố.');
    } finally {
      setIsClassifying(false);
    }
  };

  
  const handleUpdateProgress = async (e) => {
    e.preventDefault();
    setIsProgressing(true);
    setProgressError(null);
    setProgressSuccess(null);
    try {
      const payload = {
        status: progressStatus,
      };
      if (progressNote.trim()) {
        payload.note = progressNote.trim();
      }
      await incidentsApi.progressIncident(id, payload);
      setProgressSuccess('Cập nhật tiến độ khắc phục sự cố thành công!');
      setProgressNote('');
      fetchIncident();
    } catch (err) {
      setProgressError(err.message || 'Không thể cập nhật tiến độ sự cố.');
    } finally {
      setIsProgressing(false);
    }
  };

  
  const handleCloseIncident = async () => {
    if (!window.confirm('Bạn có chắc chắn muốn đóng sự cố này sau khi nghiệm thu hoàn tất?')) {
      return;
    }
    setIsClosing(true);
    setCloseError(null);
    setCloseSuccess(null);
    try {
      await incidentsApi.closeIncident(id);
      setCloseSuccess('Sự cố đã được đóng thành công!');
      fetchIncident();
    } catch (err) {
      if (err.status === 409 || err.code === 'INCIDENT_WO_NOT_CLOSED') {
        setCloseError(
          'Không thể đóng sự cố: Work Order sửa chữa liên kết chưa được nghiệm thu ĐẠT (chưa CLOSED). Vui lòng hoàn tất nghiệm thu Work Order trước khi đóng sự cố.'
        );
      } else {
        setCloseError(err.message || 'Không thể đóng sự cố.');
      }
    } finally {
      setIsClosing(false);
    }
  };

  if (loading) {
    return <LoadingState message="Đang nạp thông tin chi tiết sự cố..." />;
  }

  if (error) {
    return (
      <ErrorState
        title={error.status === 403 ? 'Truy cập bị từ chối (403 Forbidden)' : 'Lỗi tải sự cố'}
        message={
          error.status === 403
            ? 'Bạn không có quyền xem hoặc thao tác trên sự cố này (AZ-07).'
            : error.message
        }
        code={error.code}
        onRetry={fetchIncident}
      />
    );
  }

  if (!incident) return null;

  const isClosed = incident.status === 'CLOSED';
  const isResolved = incident.status === 'RESOLVED';
  const isClassified = incident.status === 'CLASSIFIED';
  const isReported = incident.status === 'REPORTED';

  return (
    <div>
      <PageHeader
        title={`Sự cố: ${incident.trackingCode || `INC-${incident.incidentId}`}`}
        subtitle={`Thiết bị: ${incident.equipmentCode} (${incident.brand || ''} ${incident.model || ''})`}
        actions={
          <Button variant="outline-secondary" size="sm" onClick={() => navigate(-1)}>
            <i className="bi bi-arrow-left me-1"></i>Quay lại danh sách
          </Button>
        }
      />

      {}
      <Card className="border border-light-subtle shadow-sm mb-4">
        <Card.Header className="bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
          <div className="d-flex align-items-center gap-2">
            <span className="fw-bold text-dark">Hồ sơ thông tin sự cố</span>
            <span className="font-monospace text-muted small">#{incident.incidentId}</span>
          </div>
          <StatusBadge type="incident" value={incident.status} />
        </Card.Header>
        <Card.Body className="p-4">
          <Row className="g-3 mb-4">
            <Col md={3}>
              <div className="text-secondary small">Mã thiết bị</div>
              <div className="fw-bold text-primary font-monospace fs-6">{incident.equipmentCode || '—'}</div>
              <div className="text-muted small">{incident.brand} {incident.model}</div>
            </Col>
            <Col md={3}>
              <div className="text-secondary small">Mức độ nghiêm trọng</div>
              <div className="mt-1">
                {incident.severity ? (
                  <Badge bg={incident.severity === 'CRITICAL' ? 'danger' : incident.severity === 'HIGH' ? 'warning' : 'info'}>
                    {incident.severity}
                  </Badge>
                ) : (
                  <Badge bg="secondary">Chưa phân loại</Badge>
                )}
              </div>
            </Col>
            <Col md={3}>
              <div className="text-secondary small">Người báo cáo</div>
              <div className="fw-semibold text-dark">{incident.reporterName || 'N/A'}</div>
              <div className="text-muted small">Mã NV: #{incident.reporterId}</div>
            </Col>
            <Col md={3}>
              <div className="text-secondary small">Thời điểm báo cáo</div>
              <div className="font-monospace small">
                {incident.createdAt ? new Date(incident.createdAt).toLocaleString('vi-VN') : '—'}
              </div>
            </Col>
          </Row>

          <div className="mb-3">
            <div className="text-secondary small fw-semibold mb-1">Hiện tượng hư hỏng được ghi nhận:</div>
            <div className="p-3 bg-light rounded border text-dark font-monospace small">
              {incident.description}
            </div>
          </div>

          {incident.closedAt && (
            <Alert variant="success" className="mb-0 small border-success">
              <i className="bi bi-check-circle-fill me-2"></i>
              Sự cố đã được hoàn tất sửa chữa và nghiệm thu đóng vào ngày{' '}
              {new Date(incident.closedAt).toLocaleString('vi-VN')}.
            </Alert>
          )}
        </Card.Body>
      </Card>

      {}
      {isManager && isReported && (
        <Card className="border border-warning shadow-sm mb-4">
          <Card.Header className="bg-warning-subtle py-2 fw-semibold text-dark d-flex align-items-center gap-2">
            <i className="bi bi-tag-fill text-warning"></i>
            <span>Thẩm định & Phân loại Mức độ Nghiêm trọng (Manager)</span>
          </Card.Header>
          <Card.Body className="p-3">
            {classifyError && <Alert variant="danger" className="small py-2">{classifyError}</Alert>}
            {classifySuccess && <Alert variant="success" className="small py-2">{classifySuccess}</Alert>}

            <Form onSubmit={handleClassify}>
              <Row className="g-3 align-items-end">
                <Col md={6}>
                  <Form.Label className="small fw-semibold">Mức độ nghiêm trọng (Severity)</Form.Label>
                  <Form.Select
                    size="sm"
                    value={selectedSeverity}
                    onChange={(e) => setSelectedSeverity(e.target.value)}
                  >
                    <option value="LOW">Thấp (LOW) — Hỏng hóc nhẹ, không ảnh hưởng vận hành</option>
                    <option value="MEDIUM">Trung bình (MEDIUM) — Cần bảo trì khắc phục sớm</option>
                    <option value="HIGH">Cao (HIGH) — Nguy cơ hỏng lan rộng hoặc sai số thiết bị</option>
                    <option value="CRITICAL">Khẩn cấp (CRITICAL) — Dừng máy ngay, mất an toàn</option>
                  </Form.Select>
                </Col>
                <Col md={6}>
                  <Button
                    type="submit"
                    variant="warning"
                    size="sm"
                    disabled={isClassifying}
                    className="d-flex align-items-center gap-2"
                  >
                    {isClassifying && <Spinner size="sm" animation="border" />}
                    <span>Xác nhận phân loại sự cố</span>
                  </Button>
                </Col>
              </Row>
            </Form>
          </Card.Body>
        </Card>
      )}

      {}
      {isManager && isClassified && (
        <Card className="border border-primary shadow-sm mb-4">
          <Card.Header className="bg-primary-subtle py-2 fw-semibold text-dark d-flex align-items-center gap-2">
            <i className="bi bi-tools text-primary"></i>
            <span>Tạo Phiếu Sửa chữa (Repair Work Order)</span>
          </Card.Header>
          <Card.Body className="p-3 d-flex justify-content-between align-items-center">
            <div>
              <p className="mb-0 small text-secondary">
                Sự cố đã được phân loại mức độ <strong>{incident.severity}</strong>. Tạo phiếu Work Order loại Sửa chữa (REPAIR) để phân công Kỹ thuật viên xử lý.
              </p>
            </div>
            <Button
              as={Link}
              to={`/manager/work-orders/new?type=REPAIR&equipmentId=${incident.equipmentId}&incidentId=${incident.incidentId}`}
              variant="primary"
              size="sm"
              className="d-flex align-items-center gap-1"
            >
              <i className="bi bi-plus-circle"></i>
              <span>Tạo Work Order sửa chữa</span>
            </Button>
          </Card.Body>
        </Card>
      )}

      {}
      {isTechnician && !isClosed && (
        <Card className="border border-info shadow-sm mb-4">
          <Card.Header className="bg-info-subtle py-2 fw-semibold text-dark d-flex align-items-center gap-2">
            <i className="bi bi-wrench-adjustable text-info"></i>
            <span>Cập nhật Tiến độ Sửa chữa (Kỹ thuật viên)</span>
          </Card.Header>
          <Card.Body className="p-3">
            {progressError && <Alert variant="danger" className="small py-2">{progressError}</Alert>}
            {progressSuccess && <Alert variant="success" className="small py-2">{progressSuccess}</Alert>}

            <Form onSubmit={handleUpdateProgress}>
              <Row className="g-3">
                <Col md={4}>
                  <Form.Label className="small fw-semibold">Trạng thái tiến độ</Form.Label>
                  <Form.Select
                    size="sm"
                    value={progressStatus}
                    onChange={(e) => setProgressStatus(e.target.value)}
                  >
                    <option value="IN_REPAIR">Đang tiến hành sửa chữa (IN_REPAIR)</option>
                    <option value="RESOLVED">Đã hoàn thành sửa chữa (RESOLVED)</option>
                  </Form.Select>
                </Col>
                <Col md={8}>
                  <Form.Label className="small fw-semibold">Ghi chú kỹ thuật / Linh kiện thay thế</Form.Label>
                  <Form.Control
                    type="text"
                    size="sm"
                    placeholder="Mô tả công việc đã xử lý hoặc ghi chú kỹ thuật..."
                    value={progressNote}
                    onChange={(e) => setProgressNote(e.target.value)}
                  />
                </Col>
                <Col xs={12} className="d-flex justify-content-between align-items-center pt-2">
                  <div className="small text-muted fst-italic">
                    * Lưu ý: Kỹ thuật viên chỉ cập nhật tiến độ và xác nhận khắc phục. Quyền đóng sự cố thuộc về Quản lý sau khi nghiệm thu.
                  </div>
                  <Button
                    type="submit"
                    variant="info"
                    size="sm"
                    disabled={isProgressing}
                    className="d-flex align-items-center gap-2 text-white"
                  >
                    {isProgressing && <Spinner size="sm" animation="border" />}
                    <span>Cập nhật tiến độ sự cố</span>
                  </Button>
                </Col>
              </Row>
            </Form>
          </Card.Body>
        </Card>
      )}

      {}
      {isManager && isResolved && (
        <Card className="border border-success shadow-sm mb-4">
          <Card.Header className="bg-success-subtle py-2 fw-semibold text-dark d-flex align-items-center gap-2">
            <i className="bi bi-check2-circle text-success"></i>
            <span>Đóng Sự cố sau Nghiệm thu Đạt (Manager)</span>
          </Card.Header>
          <Card.Body className="p-3">
            {closeError && (
              <Alert variant="danger" className="small py-2 border-danger">
                <i className="bi bi-exclamation-triangle-fill me-2"></i>
                {closeError}
              </Alert>
            )}
            {closeSuccess && (
              <Alert variant="success" className="small py-2 border-success">
                <i className="bi bi-check-circle-fill me-2"></i>
                {closeSuccess}
              </Alert>
            )}

            <div className="d-flex justify-content-between align-items-center">
              <div>
                <p className="mb-0 small text-secondary">
                  Kỹ thuật viên đã đánh dấu sự cố là <strong>RESOLVED</strong>. Sau khi hoàn tất nghiệm thu ĐẠT trên Work Order sửa chữa tương ứng, Quản lý xác nhận đóng sự cố.
                </p>
              </div>
              <Button
                variant="success"
                size="sm"
                onClick={handleCloseIncident}
                disabled={isClosing}
                className="d-flex align-items-center gap-2"
              >
                {isClosing && <Spinner size="sm" animation="border" />}
                <i className="bi bi-lock-fill"></i>
                <span>Xác nhận Đóng sự cố</span>
              </Button>
            </div>
          </Card.Body>
        </Card>
      )}
    </div>
  );
}

