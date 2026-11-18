import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Card from 'react-bootstrap/Card';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Button from 'react-bootstrap/Button';
import Alert from 'react-bootstrap/Alert';
import { useAuth } from '../../auth/AuthContext';
import { equipmentApi, statusRequestsApi } from '../../api';
import PageHeader from '../../components/common/PageHeader';
import StatusBadge from '../../components/common/StatusBadge';
import LoadingState from '../../components/common/LoadingState';
import ErrorState from '../../components/common/ErrorState';
import ConfirmDialog from '../../components/common/ConfirmDialog';

export default function EquipmentDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { role } = useAuth();

  const [equipment, setEquipment] = useState(null);
  const [plan, setPlan] = useState(null);
  const [history, setHistory] = useState([]);
  const [pendingRequest, setPendingRequest] = useState(null);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  
  const [confirmDialog, setConfirmDialog] = useState({
    show: false,
    decision: null,
    title: '',
    message: '',
    variant: 'primary',
  });
  const [decisionNote, setDecisionNote] = useState('');
  const [isDeciding, setIsDeciding] = useState(false);
  const [decisionError, setDecisionError] = useState(null);

  const getBasePath = useCallback(() => {
    switch (role) {
      case 'ADMIN':
        return '/admin';
      case 'MANAGER':
        return '/manager';
      case 'TECHNICIAN':
        return '/technician';
      case 'EQUIPMENT_USER':
      default:
        return '/user';
    }
  }, [role]);

  const basePath = getBasePath();


  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [eqRes, planRes, histRes, reqsRes] = await Promise.allSettled([
        equipmentApi.getEquipmentById(id),
        equipmentApi.getEquipmentPlan(id),
        equipmentApi.getEquipmentHistory(id, { page: 0, size: 5 }),
        equipmentApi.getStatusRequestsByEquipment(id),
      ]);

      if (eqRes.status === 'fulfilled') {
        const eqData = eqRes.value?.data || eqRes.value || {};
        setEquipment(eqData);
        
        if (eqData.pendingStatusRequest) {
          setPendingRequest(eqData.pendingStatusRequest);
        }
      } else {
        throw eqRes.reason;
      }

      if (planRes.status === 'fulfilled') {
        setPlan(planRes.value?.data || planRes.value || null);
      }

      if (histRes.status === 'fulfilled') {
        setHistory(histRes.value?.items || histRes.value?.data?.items || []);
      }

      if (reqsRes.status === 'fulfilled') {
        const reqList = reqsRes.value?.items || reqsRes.value?.data || (Array.isArray(reqsRes.value) ? reqsRes.value : []);
        const pending = reqList.find((r) => r.status === 'PENDING');
        if (pending) {
          setPendingRequest(pending);
        } else if (!eqRes.value?.pendingStatusRequest && !eqRes.value?.data?.pendingStatusRequest) {
          setPendingRequest(null);
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

  const handleOpenDecisionDialog = (decision) => {
    setDecisionNote('');
    setDecisionError(null);
    if (decision === 'CONFIRM') {
      setConfirmDialog({
        show: true,
        decision: 'CONFIRM',
        title: 'Xác nhận ngừng hoạt động (Out of Service)',
        message:
          'Bạn có chắc chắn muốn xác nhận đề xuất này? Trạng thái thiết bị sẽ chuyển sang OUT_OF_SERVICE và các hoạt động vận hành sẽ tạm ngừng.',
        variant: 'danger',
      });
    } else {
      setConfirmDialog({
        show: true,
        decision: 'REJECT',
        title: 'Từ chối đề xuất ngừng hoạt động',
        message: 'Bạn có chắc chắn muốn từ chối đề xuất ngừng hoạt động của kỹ thuật viên?',
        variant: 'secondary',
      });
    }
  };

  const handleExecuteDecision = async () => {
    if (!pendingRequest) return;
    setIsDeciding(true);
    setDecisionError(null);
    try {
      await statusRequestsApi.decideStatusRequest(pendingRequest.requestId, {
        decision: confirmDialog.decision,
        note: decisionNote.trim() || undefined,
      });
      setConfirmDialog((prev) => ({ ...prev, show: false }));
      await loadData();
    } catch (err) {
      setDecisionError(
        err.response?.data?.message || 'Đã có lỗi xảy ra khi xử lý quyết định đề xuất.'
      );
    } finally {
      setIsDeciding(false);
    }
  };

  if (loading) {
    return (
      <div className="p-5 text-center bg-white rounded-3 border">
        <LoadingState message="Đang nạp chi tiết thiết bị..." />
      </div>
    );
  }

  if (error || !equipment) {
    return (
      <ErrorState
        title="Không tìm thấy thiết bị"
        message={error?.response?.data?.message || 'Không thể tải thông tin chi tiết thiết bị.'}
        onRetry={loadData}
      />
    );
  }

  const canEdit = role === 'ADMIN' || role === 'MANAGER';
  const canViewHistory = role === 'MANAGER' || role === 'TECHNICIAN';
  const isManager = role === 'MANAGER';

  return (
    <div>
      <PageHeader
        title={`Chi tiết thiết bị: ${equipment.code}`}
        subtitle={`${equipment.brand || ''} ${equipment.model || ''} • Loại: ${
          equipment.type?.name || '—'
        }`}
        breadcrumbs={[
          { label: 'Bàn làm việc', to: `${basePath}/dashboard` },
          {
            label: 'Danh mục thiết bị',
            to: role === 'ADMIN' ? '/admin/equipment-catalog' : `${basePath}/equipment`,
          },
          { label: equipment.code },
        ]}
        actions={
          <div className="d-flex align-items-center gap-2">
            {canEdit && (
              <Button
                variant="outline-primary"
                onClick={() => navigate(`${basePath}/equipment/${id}/edit`)}
                className="d-inline-flex align-items-center gap-1"
              >
                <i className="bi bi-pencil"></i>
                <span>Chỉnh sửa</span>
              </Button>
            )}

            {canViewHistory && (
              <Button
                variant="outline-secondary"
                onClick={() => navigate(`${basePath}/equipment/${id}/history`)}
                className="d-inline-flex align-items-center gap-1"
              >
                <i className="bi bi-clock-history"></i>
                <span>Lịch sử bảo dưỡng</span>
              </Button>
            )}
          </div>
        }
      />

      
      {pendingRequest && pendingRequest.status === 'PENDING' && (
        <Alert
          variant="warning"
          className="border-warning-subtle shadow-sm mb-4 p-4 rounded-3"
          style={{ backgroundColor: '#FFFBEB' }}
        >
          <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3">
            <div>
              <div className="d-flex align-items-center gap-2 text-danger fw-bold mb-1">
                <i className="bi bi-exclamation-triangle-fill fs-5"></i>
                <span>ĐỀ XUẤT NGỪNG HOẠT ĐỘNG (OUT OF SERVICE) ĐANG CHỜ DUYỆT</span>
              </div>
              <p className="mb-1 text-dark">
                <strong>Lý do đề xuất:</strong> {pendingRequest.reason}
              </p>
              {decisionError && (
                <Alert variant="danger" className="py-1 px-2 small mb-2">
                  <i className="bi bi-exclamation-octagon-fill me-1"></i>
                  {decisionError}
                </Alert>
              )}
              <div className="small text-secondary">
                Đề xuất bởi: <strong className="text-dark">{pendingRequest.proposedByName || 'Kỹ thuật viên'}</strong>
                {pendingRequest.proposedAt && (
                  <span> lúc {new Date(pendingRequest.proposedAt).toLocaleString('vi-VN')}</span>
                )}
                {pendingRequest.woCode && (
                  <span className="ms-2">
                    (Phiếu bảo trì: <strong className="font-monospace text-primary">{pendingRequest.woCode}</strong>)
                  </span>
                )}
              </div>
            </div>

            {isManager && (
              <div className="d-flex align-items-center gap-2 flex-shrink-0">
                <Button
                  variant="danger"
                  size="sm"
                  onClick={() => handleOpenDecisionDialog('CONFIRM')}
                  className="d-inline-flex align-items-center gap-1"
                >
                  <i className="bi bi-check-lg"></i>
                  <span>Xác nhận ngừng HĐ</span>
                </Button>
                <Button
                  variant="outline-secondary"
                  size="sm"
                  onClick={() => handleOpenDecisionDialog('REJECT')}
                  className="d-inline-flex align-items-center gap-1"
                >
                  <i className="bi bi-x-lg"></i>
                  <span>Từ chối</span>
                </Button>
              </div>
            )}
          </div>
        </Alert>
      )}

      
      <Row className="g-4 mb-4">
        <Col xs={12} lg={8}>
          <Card className="border border-light-subtle shadow-sm h-100">
            <Card.Header className="bg-white py-3 border-bottom border-light-subtle">
              <h6 className="fw-bold text-dark mb-0 d-flex align-items-center gap-2">
                <i className="bi bi-info-circle text-primary"></i>
                <span>Thông số kỹ thuật & Vị trí lắp đặt</span>
              </h6>
            </Card.Header>
            <Card.Body className="p-4">
              <Row className="g-3">
                <Col xs={6} md={4}>
                  <div className="text-secondary small">Mã thiết bị</div>
                  <div className="fw-bold font-monospace fs-5 text-dark">{equipment.code}</div>
                </Col>

                <Col xs={6} md={4}>
                  <div className="text-secondary small">Trạng thái vận hành</div>
                  <div className="mt-1">
                    <StatusBadge type="equipment" value={equipment.status} />
                  </div>
                </Col>

                <Col xs={12} md={4}>
                  <div className="text-secondary small">Tình trạng hạn bảo trì</div>
                  <div className="mt-1 d-flex align-items-center gap-2">
                    <StatusBadge type="due" value={equipment.dueLabel || 'NORMAL'} />
                    {equipment.nextDueDate && (
                      <span className="small text-secondary font-monospace">
                        ({equipment.nextDueDate})
                      </span>
                    )}
                  </div>
                </Col>

                <Col xs={12} md={4}>
                  <div className="text-secondary small">Chủng loại</div>
                  <div className="fw-semibold text-dark">{equipment.type?.name || '—'}</div>
                </Col>

                <Col xs={6} md={4}>
                  <div className="text-secondary small">Hãng sản xuất</div>
                  <div className="fw-semibold text-dark">{equipment.brand || '—'}</div>
                </Col>

                <Col xs={6} md={4}>
                  <div className="text-secondary small">Model máy</div>
                  <div className="fw-semibold text-dark">{equipment.model || '—'}</div>
                </Col>

                <Col xs={12} md={4}>
                  <div className="text-secondary small">Số Serial</div>
                  <div className="fw-semibold text-dark font-monospace">{equipment.serial || '—'}</div>
                </Col>

                <Col xs={12} md={8}>
                  <div className="text-secondary small">Vị trí lắp đặt</div>
                  <div className="fw-semibold text-dark">
                    {equipment.location ? (
                      <span>
                        Phòng {equipment.location.room}
                        {equipment.location.area ? ` - ${equipment.location.area}` : ''}
                        {equipment.location.department ? ` (${equipment.location.department})` : ''}
                      </span>
                    ) : (
                      '—'
                    )}
                  </div>
                </Col>
              </Row>
            </Card.Body>
          </Card>
        </Col>

        
        <Col xs={12} lg={4}>
          <Card className="border border-light-subtle shadow-sm h-100">
            <Card.Header className="bg-white py-3 border-bottom border-light-subtle d-flex justify-content-between align-items-center">
              <h6 className="fw-bold text-dark mb-0 d-flex align-items-center gap-2">
                <i className="bi bi-calendar-check text-primary"></i>
                <span>Kế hoạch bảo trì</span>
              </h6>
              {isManager && (
                <Button
                  variant="link"
                  size="sm"
                  className="p-0 text-decoration-none"
                  onClick={() => navigate('/manager/maintenance-plans')}
                >
                  Cấu hình
                </Button>
              )}
            </Card.Header>
            <Card.Body className="p-4">
              {plan ? (
                <div className="d-flex flex-column gap-3">
                  <div>
                    <div className="text-secondary small">Chu kỳ bảo dưỡng định kỳ</div>
                    <div className="fw-bold text-dark fs-5">
                      {plan.cycleDays}{' '}
                      <span className="small text-secondary fw-normal">ngày / lần</span>
                    </div>
                  </div>

                  <div>
                    <div className="text-secondary small">Lần hoàn tất gần nhất</div>
                    <div className="fw-semibold text-dark font-monospace">
                      {plan.lastCompletedDate || 'Chưa thực hiện lần nào'}
                    </div>
                  </div>

                  <div>
                    <div className="text-secondary small">Hạn bảo trì tiếp theo (Server tính)</div>
                    <div className="fw-bold text-primary font-monospace fs-6">
                      {plan.nextDueDate || 'Chưa xác định'}
                    </div>
                  </div>
                </div>
              ) : (
                <div className="text-center py-4 text-secondary">
                  <i className="bi bi-calendar-x fs-2 text-muted mb-2 d-block"></i>
                  <p className="small mb-0">Thiết bị chưa được cấu hình kế hoạch bảo trì định kỳ.</p>
                </div>
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>

      
      {canViewHistory && (
        <Card className="border border-light-subtle shadow-sm mb-4">
          <Card.Header className="bg-white py-3 border-bottom border-light-subtle d-flex justify-content-between align-items-center">
            <h6 className="fw-bold text-dark mb-0 d-flex align-items-center gap-2">
              <i className="bi bi-journal-text text-primary"></i>
              <span>Lịch sử bảo dưỡng gần đây (5 bản ghi mới nhất)</span>
            </h6>
            <Button
              variant="link"
              size="sm"
              className="p-0 text-decoration-none"
              onClick={() => navigate(`${basePath}/equipment/${id}/history`)}
            >
              Xem toàn bộ lịch sử <i className="bi bi-arrow-right"></i>
            </Button>
          </Card.Header>
          <Card.Body className="p-0">
            {history.length > 0 ? (
              <div className="table-responsive">
                <table className="table table-hover align-middle mb-0" style={{ fontSize: '0.875rem' }}>
                  <thead className="table-light">
                    <tr>
                      <th style={{ width: '180px' }}>Thời điểm</th>
                      <th>Thao tác</th>
                      <th style={{ width: '180px' }}>Chuyển đổi trạng thái</th>
                      <th>Người thực hiện</th>
                      <th>Ghi chú</th>
                    </tr>
                  </thead>
                  <tbody>
                    {history.map((h) => (
                      <tr key={h.historyId || h.id}>
                        <td className="font-monospace text-secondary">
                          {h.performedAt ? new Date(h.performedAt).toLocaleString('vi-VN') : '—'}
                        </td>
                        <td className="fw-semibold text-dark">{h.action}</td>
                        <td>
                          {h.oldStatus || h.newStatus ? (
                            <span className="small">
                              <span className="badge bg-light text-dark border me-1">
                                {h.oldStatus || '—'}
                              </span>
                              <i className="bi bi-arrow-right text-muted me-1"></i>
                              <span className="badge bg-secondary-subtle text-dark border">
                                {h.newStatus || '—'}
                              </span>
                            </span>
                          ) : (
                            '—'
                          )}
                        </td>
                        <td className="fw-medium text-dark">{h.performedByName || 'Hệ thống'}</td>
                        <td className="text-secondary">{h.note || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="p-4 text-center text-secondary small">
                Chưa có nhật ký bảo dưỡng nào được ghi nhận cho thiết bị này.
              </div>
            )}
          </Card.Body>
        </Card>
      )}

      
      <ConfirmDialog
        show={confirmDialog.show}
        onHide={() => !isDeciding && setConfirmDialog((prev) => ({ ...prev, show: false }))}
        onConfirm={handleExecuteDecision}
        title={confirmDialog.title}
        message={confirmDialog.message}
        confirmText={confirmDialog.decision === 'CONFIRM' ? 'Xác nhận Out of Service' : 'Từ chối đề xuất'}
        variant={confirmDialog.variant}
        isLoading={isDeciding}
      />
    </div>
  );
}
