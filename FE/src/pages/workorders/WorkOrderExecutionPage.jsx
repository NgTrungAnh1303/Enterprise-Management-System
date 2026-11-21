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
import Modal from 'react-bootstrap/Modal';
import { workOrdersApi, equipmentApi } from '../../api';
import PageHeader from '../../components/common/PageHeader';
import StatusBadge from '../../components/common/StatusBadge';
import LoadingState from '../../components/common/LoadingState';
import ErrorState from '../../components/common/ErrorState';

export default function WorkOrderExecutionPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [wo, setWo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

   const [results, setResults] = useState([]);
  const [incompleteItemIds, setIncompleteItemIds] = useState(new Set());

   const [isStarting, setIsStarting] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [actionError, setActionError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);

   const [showOosModal, setShowOosModal] = useState(false);
  const [oosReason, setOosReason] = useState('');
  const [isProposingOos, setIsProposingOos] = useState(false);
  const [oosError, setOosError] = useState(null);

   const loadWorkOrder = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await workOrdersApi.getWorkOrderById(id);
      const woData = res?.data || res || {};
      setWo(woData);

       const items = (woData.inspectionResults || []).map((r) => ({
        checklistId: r.checklistId,
        item: r.item,
        isRequired: r.isRequired,
        done: Boolean(r.done),
        condition: r.condition || '',
        defect: r.defect || '',
        workDone: r.workDone || '',
      }));
      setResults(items);
      setIncompleteItemIds(new Set());
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadWorkOrder();
  }, [loadWorkOrder]);

   const handleResultChange = (checklistId, field, value) => {
    setResults((prev) =>
      prev.map((r) => (r.checklistId === checklistId ? { ...r, [field]: value } : r))
    );
    // Remove from incomplete highlights once modified
    if (field === 'done' && value === true) {
      setIncompleteItemIds((prev) => {
        const next = new Set(prev);
        next.delete(checklistId);
        return next;
      });
    }
  };

   const handleStart = async () => {
    setIsStarting(true);
    setActionError(null);
    setActionSuccess(null);
    try {
      await workOrdersApi.startWorkOrder(id);
      setActionSuccess('Đã bắt đầu thực hiện Work Order. Thiết bị chuyển sang ĐANG BẢO DƯỠNG.');
      await loadWorkOrder();
    } catch (err) {
      setActionError(err.response?.data?.message || 'Không thể bắt đầu thực hiện Work Order.');
    } finally {
      setIsStarting(false);
    }
  };

   const handleSaveResults = async () => {
    setIsSaving(true);
    setActionError(null);
    setActionSuccess(null);
    try {
      const payload = {
        items: results.map((r) => ({
          checklistId: r.checklistId,
          done: r.done,
          condition: r.condition?.trim() || null,
          defect: r.defect?.trim() || null,
          workDone: r.workDone?.trim() || null,
        })),
      };
      await workOrdersApi.updateResults(id, payload);
      setActionSuccess('Đã lưu kết quả kiểm tra thành công!');
      await loadWorkOrder();
    } catch (err) {
      setActionError(err.response?.data?.message || 'Không thể lưu kết quả kiểm tra.');
    } finally {
      setIsSaving(false);
    }
  };

   const handleSubmitWorkOrder = async () => {
    setIsSubmitting(true);
    setActionError(null);
    setActionSuccess(null);
    setIncompleteItemIds(new Set());

    try {
       const payload = {
        items: results.map((r) => ({
          checklistId: r.checklistId,
          done: r.done,
          condition: r.condition?.trim() || null,
          defect: r.defect?.trim() || null,
          workDone: r.workDone?.trim() || null,
        })),
      };
      await workOrdersApi.updateResults(id, payload);

       await workOrdersApi.submitWorkOrder(id);
      setActionSuccess('Nộp phiếu bảo trì cho Quản lý nghiệm thu thành công!');
      await loadWorkOrder();
    } catch (err) {
      const status = err.response?.status;
      const data = err.response?.data;

      if (status === 422 && data?.code === 'CHECKLIST_INCOMPLETE') {
         const missingIds = new Set();
        results.forEach((r) => {
          if (r.isRequired && !r.done) {
            missingIds.add(r.checklistId);
          }
        });
        setIncompleteItemIds(missingIds);
        setActionError(
          data.message ||
            'Không thể nộp nghiệm thu: Còn các hạng mục bắt buộc chưa hoàn thành. Các mục vi phạm đã được tô đỏ bên dưới.'
        );
      } else {
        setActionError(data?.message || 'Đã có lỗi xảy ra khi nộp nghiệm thu.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

   const handleProposeOos = async (e) => {
    e.preventDefault();
    if (!oosReason.trim()) {
      setOosError('Vui lòng nhập lý do đề xuất ngừng hoạt động.');
      return;
    }
    setIsProposingOos(true);
    setOosError(null);
    try {
      await equipmentApi.proposeOutOfService(wo.equipment?.id || wo.equipment?.equipmentId, {
        proposedStatus: 'OUT_OF_SERVICE',
        reason: oosReason.trim(),
        woId: wo.woId,
      });
      setShowOosModal(false);
      setOosReason('');
      setActionSuccess('Đã gửi đề xuất ngừng hoạt động (Out of Service) thành công!');
      await loadWorkOrder();
    } catch (err) {
      setOosError(
        err.response?.data?.message || 'Không thể gửi đề xuất ngừng hoạt động.'
      );
    } finally {
      setIsProposingOos(false);
    }
  };

  if (loading) {
    return (
      <div className="p-5 text-center bg-white rounded-3 border">
        <LoadingState message="Đang nạp chi tiết Work Order..." />
      </div>
    );
  }

  if (error || !wo) {
    const is403 = error?.response?.status === 403;
    return (
      <ErrorState
        code={is403 ? 403 : undefined}
        title={is403 ? 'Không có quyền truy cập' : 'Không tìm thấy Work Order'}
        message={
          is403
            ? 'Bạn không được phân công Work Order này hoặc không có quyền truy cập vào hồ sơ bảo trì.'
            : error?.response?.data?.message || 'Không thể tải thông tin Work Order.'
        }
        onRetry={loadWorkOrder}
      />
    );
  }

  const isAssigned = wo.status === 'ASSIGNED';
  const isInProgress = wo.status === 'IN_PROGRESS';
  const isReadOnly = wo.status === 'SUBMITTED' || wo.status === 'CLOSED';

   const latestRejection = (wo.acceptances || [])
    .filter((a) => a.decision === 'FAIL')
    .sort((a, b) => new Date(b.acceptedAt) - new Date(a.acceptedAt))[0];

  return (
    <div>
      <PageHeader
        title={`Thực hiện bảo trì: ${wo.woCode}`}
        subtitle={`Thiết bị: ${wo.equipment?.code} (${wo.equipment?.brand || ''} ${
          wo.equipment?.model || ''
        }) • Phân loại: ${wo.type === 'PERIODIC' ? 'Định kỳ' : 'Sửa chữa'}`}
        breadcrumbs={[
          { label: 'Bàn làm việc', to: '/technician/dashboard' },
          { label: 'Phiếu bảo trì', to: '/technician/work-orders' },
          { label: wo.woCode },
        ]}
        actions={
          <div className="d-flex align-items-center gap-2">
            <Button
              variant="outline-secondary"
              onClick={() => navigate('/technician/work-orders')}
              className="d-inline-flex align-items-center gap-1"
            >
              <i className="bi bi-arrow-left"></i>
              <span>Quay lại</span>
            </Button>

            {isAssigned && (
              <Button
                variant="primary"
                onClick={handleStart}
                disabled={isStarting}
                className="d-inline-flex align-items-center gap-2"
              >
                {isStarting ? (
                  <Spinner size="sm" animation="border" />
                ) : (
                  <i className="bi bi-play-circle-fill"></i>
                )}
                <span>Bắt đầu thực hiện</span>
              </Button>
            )}

            {isInProgress && (
              <>
                <Button
                  variant="outline-danger"
                  onClick={() => setShowOosModal(true)}
                  className="d-inline-flex align-items-center gap-1"
                >
                  <i className="bi bi-exclamation-octagon"></i>
                  <span>Đề xuất OOS</span>
                </Button>
                <Button
                  variant="outline-primary"
                  onClick={handleSaveResults}
                  disabled={isSaving || isSubmitting}
                  className="d-inline-flex align-items-center gap-1"
                >
                  {isSaving && <Spinner size="sm" animation="border" />}
                  <i className="bi bi-save"></i>
                  <span>Lưu kết quả</span>
                </Button>
                <Button
                  variant="success"
                  onClick={handleSubmitWorkOrder}
                  disabled={isSubmitting || isSaving}
                  className="d-inline-flex align-items-center gap-1"
                >
                  {isSubmitting && <Spinner size="sm" animation="border" />}
                  <i className="bi bi-send-check"></i>
                  <span>Nộp nghiệm thu</span>
                </Button>
              </>
            )}
          </div>
        }
      />

       {latestRejection && (
        <Alert variant="danger" className="border-danger-subtle shadow-sm mb-4 p-4 rounded-3">
          <div className="d-flex align-items-start gap-3">
            <i className="bi bi-exclamation-triangle-fill fs-4 text-danger flex-shrink-0 mt-1"></i>
            <div>
              <h6 className="fw-bold text-danger mb-1">
                PHIẾU BẢO TRÌ TỪNG BỊ TỪ CHỐI NGHIỆM THU (KHÔNG ĐẠT)
              </h6>
              <p className="mb-2 text-dark">
                <strong>Ghi chú từ người quản lý:</strong> &ldquo;{latestRejection.note}&rdquo;
              </p>
              <div className="small text-secondary">
                Đánh giá bởi:{' '}
                <strong className="text-dark">
                  {latestRejection.acceptedByName || 'Người quản lý'}
                </strong>{' '}
                lúc{' '}
                {latestRejection.acceptedAt
                  ? new Date(latestRejection.acceptedAt).toLocaleString('vi-VN')
                  : '—'}
                . Vui lòng khắc phục các điểm chưa đạt trước khi nộp lại.
              </div>
            </div>
          </div>
        </Alert>
      )}

       {actionError && (
        <Alert variant="danger" className="d-flex align-items-center gap-2 mb-4">
          <i className="bi bi-exclamation-octagon-fill fs-5 flex-shrink-0"></i>
          <div>{actionError}</div>
        </Alert>
      )}

      {actionSuccess && (
        <Alert variant="success" className="d-flex align-items-center gap-2 mb-4">
          <i className="bi bi-check-circle-fill fs-5 flex-shrink-0"></i>
          <div>{actionSuccess}</div>
        </Alert>
      )}

      {isReadOnly && (
        <Alert variant="info" className="d-flex align-items-center gap-2 mb-4">
          <i className="bi bi-lock-fill fs-5 flex-shrink-0"></i>
          <div>
            Phiếu bảo trì đang ở trạng thái <strong>{wo.status}</strong>. Toàn bộ hồ sơ kiểm tra
            đang ở chế độ <strong>chỉ đọc (Read-only)</strong> để đảm bảo tính bất biến và toàn vẹn dữ liệu.
          </div>
        </Alert>
      )}

       <Card className="border border-light-subtle shadow-sm mb-4">
        <Card.Body className="p-4">
          <Row className="g-3">
            <Col xs={6} md={3}>
              <div className="text-secondary small">Mã Work Order</div>
              <div className="fw-bold font-monospace fs-5 text-dark">{wo.woCode}</div>
            </Col>

            <Col xs={6} md={3}>
              <div className="text-secondary small">Trạng thái phiếu</div>
              <div className="mt-1">
                <StatusBadge type="workOrder" value={wo.status} />
              </div>
            </Col>

            <Col xs={6} md={3}>
              <div className="text-secondary small">Kỳ bảo trì (Period)</div>
              <div className="fw-semibold font-monospace text-dark">{wo.period || '—'}</div>
            </Col>

            <Col xs={6} md={3}>
              <div className="text-secondary small">Vị trí lắp đặt</div>
              <div className="fw-semibold text-dark">
                {wo.equipment?.location
                  ? `${wo.equipment.location.room} (${wo.equipment.location.area || ''})`
                  : '—'}
              </div>
            </Col>

            {wo.incidentTrackingCode && (
              <Col xs={12} md={6}>
                <div className="text-secondary small">Sự cố kỹ thuật liên quan</div>
                <div className="fw-semibold text-danger font-monospace">
                  [{wo.incidentTrackingCode}]
                </div>
              </Col>
            )}

            <Col xs={12} md={6}>
              <div className="text-secondary small">Phân công bởi</div>
              <div className="fw-semibold text-dark">
                {wo.currentAssignment?.assignedByName || 'Quản lý'}{' '}
                {wo.currentAssignment?.assignedAt && (
                  <span className="small text-secondary font-monospace">
                    (lúc {new Date(wo.currentAssignment.assignedAt).toLocaleString('vi-VN')})
                  </span>
                )}
              </div>
            </Col>
          </Row>
        </Card.Body>
      </Card>

       <Card className="border border-light-subtle shadow-sm mb-4">
        <Card.Header className="bg-white py-3 border-bottom border-light-subtle d-flex justify-content-between align-items-center">
          <h6 className="fw-bold text-dark mb-0 d-flex align-items-center gap-2">
            <i className="bi bi-list-check text-primary"></i>
            <span>Nội dung kiểm tra & Kết quả bảo trì ({results.length} hạng mục)</span>
          </h6>
          {isAssigned && (
            <Badge bg="warning-subtle" className="text-warning-emphasis border border-warning-subtle">
              Cần nhấn &ldquo;Bắt đầu thực hiện&rdquo; để nhập kết quả
            </Badge>
          )}
        </Card.Header>
        <Card.Body className="p-0">
          {results.length > 0 ? (
            <div className="table-responsive">
              <table className="table table-bordered align-middle mb-0" style={{ fontSize: '0.875rem' }}>
                <thead className="table-light">
                  <tr>
                    <th style={{ width: '48px' }} className="text-center">
                      Xong
                    </th>
                    <th style={{ width: '280px' }}>Hạng mục kiểm tra</th>
                    <th>Tình trạng thực tế (Condition)</th>
                    <th>Lỗi phát hiện (Defect)</th>
                    <th>Công việc đã xử lý (Work Done)</th>
                  </tr>
                </thead>
                <tbody>
                  {results.map((r, idx) => {
                    const isIncomplete = incompleteItemIds.has(r.checklistId);
                    return (
                      <tr
                        key={r.checklistId || idx}
                        className={isIncomplete ? 'table-danger' : undefined}
                      >
                        <td className="text-center">
                          <Form.Check
                            type="checkbox"
                            checked={r.done}
                            disabled={!isInProgress}
                            onChange={(e) =>
                              handleResultChange(r.checklistId, 'done', e.target.checked)
                            }
                            aria-label={`Hoàn thành hạng mục ${r.item}`}
                          />
                        </td>
                        <td>
                          <div className="fw-medium text-dark">{r.item}</div>
                          <div className="mt-1">
                            {r.isRequired ? (
                              <Badge
                                bg="danger-subtle"
                                className="text-danger border border-danger-subtle"
                                style={{ fontSize: '0.7rem' }}
                              >
                                Bắt buộc
                              </Badge>
                            ) : (
                              <Badge
                                bg="secondary-subtle"
                                className="text-secondary border"
                                style={{ fontSize: '0.7rem' }}
                              >
                                Tùy chọn
                              </Badge>
                            )}
                            {isIncomplete && (
                              <span className="text-danger small ms-2 fw-semibold">
                                * Chưa hoàn tất
                              </span>
                            )}
                          </div>
                        </td>
                        <td>
                          <Form.Control
                            size="sm"
                            type="text"
                            placeholder="Ghi nhận tình trạng..."
                            value={r.condition}
                            disabled={!isInProgress}
                            onChange={(e) =>
                              handleResultChange(r.checklistId, 'condition', e.target.value)
                            }
                          />
                        </td>
                        <td>
                          <Form.Control
                            size="sm"
                            type="text"
                            placeholder="Mô tả lỗi nếu có..."
                            value={r.defect}
                            disabled={!isInProgress}
                            onChange={(e) =>
                              handleResultChange(r.checklistId, 'defect', e.target.value)
                            }
                          />
                        </td>
                        <td>
                          <Form.Control
                            size="sm"
                            type="text"
                            placeholder="Nội dung đã khắc phục/bôi trơn..."
                            value={r.workDone}
                            disabled={!isInProgress}
                            onChange={(e) =>
                              handleResultChange(r.checklistId, 'workDone', e.target.value)
                            }
                          />
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="p-4 text-center text-secondary small">
              Work Order này không có danh mục checklist kiểm tra.
            </div>
          )}
        </Card.Body>
      </Card>

       <Modal show={showOosModal} onHide={() => !isProposingOos && setShowOosModal(false)} centered>
        <Form onSubmit={handleProposeOos}>
          <Modal.Header closeButton={!isProposingOos} className="border-bottom border-light-subtle">
            <Modal.Title className="fs-6 fw-bold text-danger d-flex align-items-center gap-2">
              <i className="bi bi-exclamation-octagon-fill"></i>
              <span>Đề xuất ngừng hoạt động (Out of Service)</span>
            </Modal.Title>
          </Modal.Header>
          <Modal.Body className="p-4">
            {oosError && (
              <Alert variant="danger" className="py-2 small mb-3">
                {oosError}
              </Alert>
            )}
            <p className="small text-secondary mb-3">
              Gửi đề xuất cho Người quản lý để chuyển trạng thái thiết bị sang{' '}
              <strong>OUT_OF_SERVICE</strong> khi phát hiện hư hại nghiêm trọng không thể khắc phục ngay
              trong ca bảo trì này.
            </p>
            <Form.Group className="mb-3">
              <Form.Label className="small fw-semibold">
                Lý do đề xuất <span className="text-danger">*</span>
              </Form.Label>
              <Form.Control
                as="textarea"
                rows={3}
                placeholder="Mô tả chi tiết nguyên nhân mất an toàn, hỏng linh kiện chính..."
                value={oosReason}
                onChange={(e) => setOosReason(e.target.value)}
                required
                autoFocus
              />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer className="border-top border-light-subtle">
            <Button
              variant="outline-secondary"
              size="sm"
              onClick={() => setShowOosModal(false)}
              disabled={isProposingOos}
            >
              Hủy
            </Button>
            <Button
              variant="danger"
              size="sm"
              type="submit"
              disabled={isProposingOos}
              className="d-inline-flex align-items-center gap-1"
            >
              {isProposingOos && <Spinner size="sm" animation="border" />}
              <span>Gửi đề xuất</span>
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </div>
  );
}
