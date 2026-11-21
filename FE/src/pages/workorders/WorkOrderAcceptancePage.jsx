import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Card from 'react-bootstrap/Card';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import Alert from 'react-bootstrap/Alert';
import Badge from 'react-bootstrap/Badge';
import Spinner from 'react-bootstrap/Spinner';
import { workOrdersApi, usersApi } from '../../api';
import PageHeader from '../../components/common/PageHeader';
import StatusBadge from '../../components/common/StatusBadge';
import LoadingState from '../../components/common/LoadingState';
import ErrorState from '../../components/common/ErrorState';

export default function WorkOrderAcceptancePage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [wo, setWo] = useState(null);
  const [adjustments, setAdjustments] = useState([]);
  const [technicians, setTechnicians] = useState([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

 
  const [decision, setDecision] = useState('PASS');
  const [acceptanceNote, setAcceptanceNote] = useState('');
  const [isAccepting, setIsAccepting] = useState(false);
  const [acceptanceError, setAcceptanceError] = useState(null);
  const [acceptanceSuccess, setAcceptanceSuccess] = useState(null);

  
  const [adjustmentReason, setAdjustmentReason] = useState('');
  const [adjustmentDetail, setAdjustmentDetail] = useState('');
  const [isAdjusting, setIsAdjusting] = useState(false);
  const [adjustmentError, setAdjustmentError] = useState(null);
  const [adjustmentSuccess, setAdjustmentSuccess] = useState(null);

  
  const [selectedTechId, setSelectedTechId] = useState('');
  const [isAssigning, setIsAssigning] = useState(false);
  const [assignError, setAssignError] = useState(null);
  const [assignSuccess, setAssignSuccess] = useState(null);

  
  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [woRes, techRes] = await Promise.all([
        workOrdersApi.getWorkOrderById(id),
        usersApi.getActiveTechnicians(),
      ]);

      const woData = woRes?.data || woRes || {};
      setWo(woData);

      
      const techList = Array.isArray(techRes) ? techRes : (techRes?.items || techRes?.data || []);
      setTechnicians(techList);

      if (woData.currentAssignment?.technicianId) {
        setSelectedTechId(String(woData.currentAssignment.technicianId));
      }

      
      if (woData.status === 'CLOSED') {
        try {
          const adjRes = await workOrdersApi.getAdjustments(id);
          setAdjustments(adjRes?.items || adjRes?.data || (Array.isArray(adjRes) ? adjRes : []));
        } catch (adjErr) {
          console.warn('Không thể nạp danh sách điều chỉnh:', adjErr);
        }
      }
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadData();
  }, [loadData]);

 
  const handleAcceptanceSubmit = async (e) => {
    e.preventDefault();
    if (decision === 'FAIL' && !acceptanceNote.trim()) {
      setAcceptanceError(
        'Quy tắc nghiệp vụ: Khi đánh giá KHÔNG ĐẠT, bắt buộc phải nhập ghi chú giải thích lý do từ chối (BR-EM-11).'
      );
      return;
    }

    setIsAccepting(true);
    setAcceptanceError(null);
    setAcceptanceSuccess(null);

    try {
      await workOrdersApi.acceptWorkOrder(id, {
        decision,
        note: acceptanceNote.trim() || undefined,
      });

      setAcceptanceSuccess(
        decision === 'PASS'
          ? 'Nghiệm thu ĐẠT thành công! Work Order đã được ĐÓNG và hệ thống đã cập nhật chu kỳ bảo trì tiếp theo.'
          : 'Đã từ chối nghiệm thu. Work Order được chuyển về IN_PROGRESS để Kỹ thuật viên khắc phục.'
      );
      setAcceptanceNote('');
      
      await loadData();
    } catch (err) {
      const status = err.response?.status;
      const data = err.response?.data;

      if (status === 403) {
        if (data?.code === 'SELF_ACCEPT' || data?.message?.includes('tự nghiệm thu')) {
          setAcceptanceError(
            'Vi phạm chính sách an ninh (BR-EM-08): Người quản lý không được tự nghiệm thu Work Order do chính mình thực hiện.'
          );
        } else {
          setAcceptanceError(data?.message || 'Bạn không có quyền thực hiện nghiệm thu Work Order này.');
        }
      } else if (status === 409) {
        setAcceptanceError(
          data?.message || 'Chỉ có thể nghiệm thu Work Order khi đang ở trạng thái SUBMITTED.'
        );
      } else {
        setAcceptanceError(data?.message || 'Đã có lỗi xảy ra khi xử lý nghiệm thu.');
      }
    } finally {
      setIsAccepting(false);
    }
  };

  
  const handleCreateAdjustment = async (e) => {
    e.preventDefault();
    if (!adjustmentReason.trim()) {
      setAdjustmentError('Vui lòng nhập lý do điều chỉnh.');
      return;
    }

    setIsAdjusting(true);
    setAdjustmentError(null);
    setAdjustmentSuccess(null);

    try {
      await workOrdersApi.adjustWorkOrder(id, {
        reason: adjustmentReason.trim(),
        changeDetail: adjustmentDetail.trim() || undefined,
      });

      setAdjustmentSuccess('Đã tạo bản ghi điều chỉnh Work Order thành công!');
      setAdjustmentReason('');
      setAdjustmentDetail('');

      
      const adjRes = await workOrdersApi.getAdjustments(id);
      setAdjustments(adjRes?.items || adjRes?.data || (Array.isArray(adjRes) ? adjRes : []));
    } catch (err) {
      setAdjustmentError(
        err.response?.data?.message || 'Không thể tạo bản ghi điều chỉnh cho Work Order.'
      );
    } finally {
      setIsAdjusting(false);
    }
  };

  
  const handleAssignTechnician = async (e) => {
    e.preventDefault();
    if (!selectedTechId) {
      setAssignError('Vui lòng chọn Kỹ thuật viên.');
      return;
    }

    setIsAssigning(true);
    setAssignError(null);
    setAssignSuccess(null);

    try {
      await workOrdersApi.assignWorkOrder(id, {
        technicianId: Number(selectedTechId),
      });
      setAssignSuccess('Đã phân công Kỹ thuật viên thành công!');
      await loadData();
    } catch (err) {
      setAssignError(err.response?.data?.message || 'Không thể phân công Kỹ thuật viên.');
    } finally {
      setIsAssigning(false);
    }
  };

  if (loading) {
    return (
      <div className="p-5 text-center bg-white rounded-3 border">
        <LoadingState message="Đang nạp chi tiết Work Order để nghiệm thu..." />
      </div>
    );
  }

  if (error || !wo) {
    return (
      <ErrorState
        title="Không tìm thấy Work Order"
        message={error?.response?.data?.message || 'Không thể nạp thông tin Work Order.'}
        onRetry={loadData}
      />
    );
  }

  const isSubmitted = wo.status === 'SUBMITTED';
  const isClosed = wo.status === 'CLOSED';
  const canAssign = wo.status === 'OPEN' || wo.status === 'ASSIGNED';

  return (
    <div>
      <PageHeader
        title={`Nghiệm thu & Quản lý: ${wo.woCode}`}
        subtitle={`Thiết bị: ${wo.equipment?.code} • ${wo.equipment?.brand || ''} ${
          wo.equipment?.model || ''
        } • Trạng thái: ${wo.status}`}
        breadcrumbs={[
          { label: 'Bàn làm việc', to: '/manager/dashboard' },
          { label: 'Phiếu bảo trì', to: '/manager/work-orders' },
          { label: wo.woCode },
        ]}
        actions={
          <Button
            variant="outline-secondary"
            onClick={() => navigate('/manager/work-orders')}
            className="d-inline-flex align-items-center gap-1"
          >
            <i className="bi bi-arrow-left"></i>
            <span>Quay lại danh sách</span>
          </Button>
        }
      />

     
      <Card className="border border-light-subtle shadow-sm mb-4">
        <Card.Body className="p-4">
          <Row className="g-3">
            <Col xs={6} md={3}>
              <div className="text-secondary small">Mã Work Order</div>
              <div className="fw-bold font-monospace fs-5 text-dark">{wo.woCode}</div>
            </Col>

            <Col xs={6} md={3}>
              <div className="text-secondary small">Trạng thái hiện tại</div>
              <div className="mt-1">
                <StatusBadge type="workOrder" value={wo.status} />
              </div>
            </Col>

            <Col xs={6} md={3}>
              <div className="text-secondary small">Phân loại</div>
              <div className="mt-1">
                <Badge
                  bg={wo.type === 'PERIODIC' ? 'info-subtle' : 'warning-subtle'}
                  className={`border ${
                    wo.type === 'PERIODIC'
                      ? 'text-info-emphasis border-info-subtle'
                      : 'text-warning-emphasis border-warning-subtle'
                  }`}
                >
                  {wo.type === 'PERIODIC' ? 'Bảo trì định kỳ' : 'Sửa chữa sự cố'}
                </Badge>
              </div>
            </Col>

            <Col xs={6} md={3}>
              <div className="text-secondary small">Kỳ bảo trì (Period)</div>
              <div className="fw-semibold font-monospace text-dark">{wo.period || '—'}</div>
            </Col>

            <Col xs={12} md={6}>
              <div className="text-secondary small">Kỹ thuật viên thực hiện</div>
              <div className="fw-semibold text-dark">
                {wo.currentAssignment?.technicianName || <span className="text-muted">Chưa phân công</span>}
                {wo.currentAssignment?.assignedAt && (
                  <span className="small text-secondary font-monospace ms-2">
                    (Phân công lúc: {new Date(wo.currentAssignment.assignedAt).toLocaleString('vi-VN')})
                  </span>
                )}
              </div>
            </Col>

            <Col xs={12} md={6}>
              <div className="text-secondary small">Vị trí lắp đặt thiết bị</div>
              <div className="fw-semibold text-dark">
                {wo.equipment?.location
                  ? `${wo.equipment.location.room} (${wo.equipment.location.area || ''})`
                  : '—'}
              </div>
            </Col>
          </Row>
        </Card.Body>
      </Card>

      
      <Card className="border border-light-subtle shadow-sm mb-4">
        <Card.Header className="bg-white py-3 border-bottom border-light-subtle">
          <h6 className="fw-bold text-dark mb-0 d-flex align-items-center gap-2">
            <i className="bi bi-card-checklist text-primary"></i>
            <span>Kết quả kiểm tra từ Kỹ thuật viên ({wo.inspectionResults?.length || 0} mục)</span>
          </h6>
        </Card.Header>
        <Card.Body className="p-0">
          {wo.inspectionResults && wo.inspectionResults.length > 0 ? (
            <div className="table-responsive">
              <table className="table table-bordered align-middle mb-0" style={{ fontSize: '0.875rem' }}>
                <thead className="table-light">
                  <tr>
                    <th style={{ width: '48px' }} className="text-center">
                      Xong
                    </th>
                    <th style={{ width: '260px' }}>Hạng mục kiểm tra</th>
                    <th>Tình trạng thực tế (Condition)</th>
                    <th>Lỗi phát hiện (Defect)</th>
                    <th>Công việc đã thực hiện (Work Done)</th>
                  </tr>
                </thead>
                <tbody>
                  {wo.inspectionResults.map((r, idx) => (
                    <tr key={r.checklistId || idx}>
                      <td className="text-center">
                        {r.done ? (
                          <i className="bi bi-check-circle-fill text-success fs-5"></i>
                        ) : (
                          <i className="bi bi-dash-circle text-secondary fs-5"></i>
                        )}
                      </td>
                      <td>
                        <div className="fw-medium text-dark">{r.item}</div>
                        {r.isRequired && (
                          <Badge
                            bg="danger-subtle"
                            className="text-danger border border-danger-subtle mt-1"
                            style={{ fontSize: '0.7rem' }}
                          >
                            Bắt buộc
                          </Badge>
                        )}
                      </td>
                      <td className="text-dark">{r.condition || <span className="text-muted">—</span>}</td>
                      <td className="text-danger">{r.defect || <span className="text-muted">—</span>}</td>
                      <td className="text-dark">{r.workDone || <span className="text-muted">—</span>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="p-4 text-center text-secondary small">
              Chưa có dữ liệu kết quả kiểm tra nào.
            </div>
          )}
        </Card.Body>
      </Card>

      
      {isSubmitted && (
        <Card className="border border-success-subtle shadow-sm mb-4">
          <Card.Header className="bg-success-subtle py-3 border-bottom border-success-subtle">
            <h6 className="fw-bold text-success mb-0 d-flex align-items-center gap-2">
              <i className="bi bi-check2-circle fs-5"></i>
              <span>Hội đồng nghiệm thu Work Order</span>
            </h6>
          </Card.Header>
          <Card.Body className="p-4">
            {acceptanceError && (
              <Alert variant="danger" className="d-flex align-items-center gap-2 mb-3">
                <i className="bi bi-exclamation-octagon-fill fs-5 flex-shrink-0"></i>
                <div>{acceptanceError}</div>
              </Alert>
            )}

            {acceptanceSuccess && (
              <Alert variant="success" className="d-flex align-items-center gap-2 mb-3">
                <i className="bi bi-check-circle-fill fs-5 flex-shrink-0"></i>
                <div>{acceptanceSuccess}</div>
              </Alert>
            )}

            <Form onSubmit={handleAcceptanceSubmit}>
              <Form.Group className="mb-4">
                <Form.Label className="small fw-bold d-block text-dark">
                  Đánh giá chất lượng thực hiện <span className="text-danger">*</span>
                </Form.Label>
                <div className="d-flex gap-4 mt-2">
                  <Form.Check
                    type="radio"
                    id="decision-pass"
                    name="decision"
                    label={
                      <span>
                        <strong className="text-success">ĐẠT (PASS)</strong> — Nghiệm thu hoàn tất,
                        đóng Work Order và chuyển thiết bị về ACTIVE
                      </span>
                    }
                    value="PASS"
                    checked={decision === 'PASS'}
                    onChange={(e) => setDecision(e.target.value)}
                  />
                  <Form.Check
                    type="radio"
                    id="decision-fail"
                    name="decision"
                    label={
                      <span>
                        <strong className="text-danger">KHÔNG ĐẠT (FAIL)</strong> — Trả về cho Kỹ
                        thuật viên khắc phục lại (Bắt buộc ghi chú)
                      </span>
                    }
                    value="FAIL"
                    checked={decision === 'FAIL'}
                    onChange={(e) => setDecision(e.target.value)}
                  />
                </div>
              </Form.Group>

              <Form.Group className="mb-4">
                <Form.Label className="small fw-bold text-dark">
                  Ghi chú nghiệm thu {decision === 'FAIL' && <span className="text-danger">* (Bắt buộc khi KHÔNG ĐẠT)</span>}
                </Form.Label>
                <Form.Control
                  as="textarea"
                  rows={3}
                  placeholder={
                    decision === 'FAIL'
                      ? 'Nêu rõ các hạng mục chưa đạt yêu cầu, sai sót kỹ thuật hoặc yêu cầu kiểm tra lại...'
                      : 'Ý kiến nhận xét hoặc ghi chú bổ sung của người nghiệm thu (tùy chọn)...'
                  }
                  value={acceptanceNote}
                  onChange={(e) => setAcceptanceNote(e.target.value)}
                  required={decision === 'FAIL'}
                />
              </Form.Group>

              <div className="d-flex justify-content-end">
                <Button
                  type="submit"
                  variant={decision === 'PASS' ? 'success' : 'danger'}
                  disabled={isAccepting}
                  className="d-inline-flex align-items-center gap-2"
                >
                  {isAccepting && <Spinner size="sm" animation="border" />}
                  <span>Xác nhận kết quả nghiệm thu</span>
                </Button>
              </div>
            </Form>
          </Card.Body>
        </Card>
      )}

      
      {isClosed && (
        <>
          <Alert variant="secondary" className="border shadow-sm mb-4 p-4 rounded-3">
            <div className="d-flex align-items-start gap-3">
              <i className="bi bi-shield-lock-fill fs-3 text-secondary flex-shrink-0"></i>
              <div>
                <h6 className="fw-bold text-dark mb-1">
                  WORK ORDER ĐÃ ĐÓNG (CLOSED) VÀ CÓ TÍNH CHẤT BẤT BIẾN
                </h6>
                <p className="mb-0 text-secondary small">
                  Theo tiêu chuẩn an ninh hệ thống (IA-EM03), toàn bộ thông tin kết quả bảo dưỡng và
                  nghiệm thu của Work Order đã đóng tuyệt đối không thể sửa đổi hoặc xóa bỏ. Mọi điều
                  chỉnh kỹ thuật phát sinh bắt buộc phải tạo bản ghi điều chỉnh riêng biệt (Append-only)
                  kèm lý do giải trình.
                </p>
              </div>
            </div>
          </Alert>

          
          <Card className="border border-light-subtle shadow-sm mb-4">
            <Card.Header className="bg-light-subtle py-3 border-bottom border-light-subtle">
              <h6 className="fw-bold text-dark mb-0 d-flex align-items-center gap-2">
                <i className="bi bi-pencil-square text-primary"></i>
                <span>Tạo bản ghi điều chỉnh (Work Order Adjustment - API-23)</span>
              </h6>
            </Card.Header>
            <Card.Body className="p-4">
              {adjustmentError && (
                <Alert variant="danger" className="py-2 small mb-3">
                  {adjustmentError}
                </Alert>
              )}
              {adjustmentSuccess && (
                <Alert variant="success" className="py-2 small mb-3">
                  {adjustmentSuccess}
                </Alert>
              )}

              <Form onSubmit={handleCreateAdjustment}>
                <Form.Group className="mb-3">
                  <Form.Label className="small fw-semibold">
                    Lý do điều chỉnh <span className="text-danger">*</span>
                  </Form.Label>
                  <Form.Control
                    type="text"
                    placeholder="Ví dụ: Bổ sung linh kiện thay thế phát sinh, đính chính chỉ số hiệu chuẩn..."
                    value={adjustmentReason}
                    onChange={(e) => setAdjustmentReason(e.target.value)}
                    required
                  />
                </Form.Group>

                <Form.Group className="mb-3">
                  <Form.Label className="small fw-semibold">Chi tiết thay đổi / Bổ sung</Form.Label>
                  <Form.Control
                    as="textarea"
                    rows={3}
                    placeholder="Mô tả cụ thể nội dung điều chỉnh hoặc thông số kỹ thuật mới..."
                    value={adjustmentDetail}
                    onChange={(e) => setAdjustmentDetail(e.target.value)}
                  />
                </Form.Group>

                <div className="d-flex justify-content-end">
                  <Button
                    type="submit"
                    variant="outline-primary"
                    disabled={isAdjusting || !adjustmentReason.trim()}
                    className="d-inline-flex align-items-center gap-2"
                  >
                    {isAdjusting && <Spinner size="sm" animation="border" />}
                    <span>Lưu bản ghi điều chỉnh</span>
                  </Button>
                </div>
              </Form>
            </Card.Body>
          </Card>

          
          <Card className="border border-light-subtle shadow-sm mb-4">
            <Card.Header className="bg-white py-3 border-bottom border-light-subtle">
              <h6 className="fw-bold text-dark mb-0 d-flex align-items-center gap-2">
                <i className="bi bi-clock-history text-primary"></i>
                <span>Lịch sử các lần điều chỉnh ({adjustments.length})</span>
              </h6>
            </Card.Header>
            <Card.Body className="p-0">
              {adjustments.length > 0 ? (
                <div className="table-responsive">
                  <table className="table table-hover align-middle mb-0" style={{ fontSize: '0.875rem' }}>
                    <thead className="table-light">
                      <tr>
                        <th style={{ width: '180px' }}>Thời điểm</th>
                        <th style={{ width: '180px' }}>Người tạo</th>
                        <th>Lý do điều chỉnh</th>
                        <th>Nội dung thay đổi</th>
                      </tr>
                    </thead>
                    <tbody>
                      {adjustments.map((adj) => (
                        <tr key={adj.adjustmentId || adj.id}>
                          <td className="font-monospace text-secondary">
                            {adj.createdAt ? new Date(adj.createdAt).toLocaleString('vi-VN') : '—'}
                          </td>
                          <td className="fw-semibold text-dark">{adj.createdByName || 'Người quản lý'}</td>
                          <td className="fw-medium text-dark">{adj.reason}</td>
                          <td className="text-secondary">{adj.changeDetail || '—'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="p-4 text-center text-secondary small">
                  Chưa có bản ghi điều chỉnh nào cho Work Order này.
                </div>
              )}
            </Card.Body>
          </Card>
        </>
      )}

      
      {canAssign && (
        <Card className="border border-light-subtle shadow-sm mb-4">
          <Card.Header className="bg-white py-3 border-bottom border-light-subtle">
            <h6 className="fw-bold text-dark mb-0 d-flex align-items-center gap-2">
              <i className="bi bi-person-gear text-primary"></i>
              <span>Điều phối & Phân công Kỹ thuật viên (API-18)</span>
            </h6>
          </Card.Header>
          <Card.Body className="p-4">
            {assignError && (
              <Alert variant="danger" className="py-2 small mb-3">
                {assignError}
              </Alert>
            )}
            {assignSuccess && (
              <Alert variant="success" className="py-2 small mb-3">
                {assignSuccess}
              </Alert>
            )}

            <Form onSubmit={handleAssignTechnician}>
              <Row className="g-3 align-items-end">
                <Col xs={12} md={8}>
                  <Form.Group>
                    <Form.Label className="small fw-semibold">
                      Chọn Kỹ thuật viên phụ trách (Dropdown chỉ nạp role TECHNICIAN)
                    </Form.Label>
                    <Form.Select
                      value={selectedTechId}
                      onChange={(e) => setSelectedTechId(e.target.value)}
                      required
                    >
                      <option value="">-- Chọn kỹ thuật viên --</option>
                      {technicians.map((t) => (
                        <option key={t.userId || t.id} value={t.userId || t.id}>
                          {t.fullName}{t.username ? ` (@${t.username})` : ''}
                        </option>
                      ))}
                    </Form.Select>
                  </Form.Group>
                </Col>
                <Col xs={12} md={4}>
                  <Button
                    type="submit"
                    variant="primary"
                    disabled={isAssigning || !selectedTechId}
                    className="w-100 d-inline-flex align-items-center justify-content-center gap-2"
                  >
                    {isAssigning && <Spinner size="sm" animation="border" />}
                    <span>{wo.status === 'ASSIGNED' ? 'Đổi phân công' : 'Phân công ngay'}</span>
                  </Button>
                </Col>
              </Row>
            </Form>
          </Card.Body>
        </Card>
      )}

      
      {wo.status === 'IN_PROGRESS' && (
        <Alert variant="warning" className="border-warning-subtle shadow-sm p-4 rounded-3">
          <div className="d-flex align-items-center gap-3">
            <i className="bi bi-clock-history fs-4 text-warning flex-shrink-0"></i>
            <div>
              <h6 className="fw-bold text-dark mb-1">WORK ORDER ĐANG TRONG QUÁ TRÌNH THỰC HIỆN</h6>
              <div className="small text-secondary">
                Kỹ thuật viên <strong>{wo.currentAssignment?.technicianName || 'phụ trách'}</strong>{' '}
                đang tiến hành kiểm tra checklist và khắc phục kỹ thuật. Nút Nghiệm thu sẽ tự động kích
                hoạt khi phiếu được nộp (SUBMITTED).
              </div>
            </div>
          </div>
        </Alert>
      )}
    </div>
  );
}

