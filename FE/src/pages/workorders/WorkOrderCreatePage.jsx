import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, useSearchParams } from 'react-router-dom';
import Card from 'react-bootstrap/Card';
import Form from 'react-bootstrap/Form';
import Button from 'react-bootstrap/Button';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Alert from 'react-bootstrap/Alert';
import Spinner from 'react-bootstrap/Spinner';
import Badge from 'react-bootstrap/Badge';
import { workOrdersApi, equipmentApi, usersApi, incidentsApi } from '../../api';
import PageHeader from '../../components/common/PageHeader';

export default function WorkOrderCreatePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();

  
  const stateData = location.state || {};
  const prefillType = searchParams.get('type') || stateData.type || 'PERIODIC';
  const prefillEqId = searchParams.get('equipmentId') || stateData.equipmentId || '';
  const prefillIncidentId = searchParams.get('incidentId') || stateData.incidentId || '';

  
  const [type, setType] = useState(prefillType);
  const [equipmentId, setEquipmentId] = useState(prefillEqId ? String(prefillEqId) : '');
  const [incidentId, setIncidentId] = useState(prefillIncidentId ? String(prefillIncidentId) : '');
  const [technicianId, setTechnicianId] = useState('');


  const [equipmentList, setEquipmentList] = useState([]);
  const [technicians, setTechnicians] = useState([]);
  const [openIncidents, setOpenIncidents] = useState([]);
  const [selectedEqPlan, setSelectedEqPlan] = useState(null);

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState(null);

  
  useEffect(() => {
    let isMounted = true;
    const loadFormData = async () => {
      setLoading(true);
      try {
        const [eqRes, techRes, incRes] = await Promise.all([
          equipmentApi.getEquipment({ size: 100 }),
          usersApi.getActiveTechnicians(),
          incidentsApi.getIncidents({ size: 100 }),
        ]);

        if (isMounted) {
          const eqItems = eqRes?.items || eqRes?.data?.items || [];
          setEquipmentList(eqItems);

          
          const techList = Array.isArray(techRes) ? techRes : (techRes?.items || techRes?.data || []);
          setTechnicians(techList);

           
          const incItems = incRes?.items || incRes?.data?.items || [];
          const activeIncs = incItems.filter((i) => i.status !== 'CLOSED');
          setOpenIncidents(activeIncs);
        }
      } catch {
        if (isMounted) {
          setServerError('Không thể nạp dữ liệu ban đầu cho form tạo Work Order.');
        }
      } finally {
        if (isMounted) setLoading(false);
      }
    };
    loadFormData();
    return () => {
      isMounted = false;
    };
  }, []);

   
  useEffect(() => {
    let isMounted = true;
    const fetchPlan = async () => {
      if (!equipmentId) {
        setSelectedEqPlan(null);
        return;
      }
      try {
        const res = await equipmentApi.getEquipmentPlan(equipmentId);
        if (isMounted) {
          setSelectedEqPlan(res?.data || res || null);
        }
      } catch {
        if (isMounted) setSelectedEqPlan(null);
      }
    };
    fetchPlan();
    return () => {
      isMounted = false;
    };
  }, [equipmentId]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!equipmentId) {
      setServerError('Vui lòng chọn thiết bị.');
      return;
    }

    if (type === 'REPAIR' && !incidentId) {
      setServerError('Phiếu sửa chữa (REPAIR) bắt buộc phải gắn với một sự cố kỹ thuật.');
      return;
    }

    setSubmitting(true);
    setServerError(null);

    try {
      const payload = {
        equipmentId: Number(equipmentId),
        type,
      };

      if (type === 'REPAIR') {
        payload.incidentId = Number(incidentId);
      }

       
      const res = await workOrdersApi.createWorkOrder(payload);
      const createdWo = res?.data || res;
      const newWoId = createdWo.woId || createdWo.id;

       if (technicianId && newWoId) {
        await workOrdersApi.assignWorkOrder(newWoId, {
          technicianId: Number(technicianId),
        });
      }

       navigate('/manager/work-orders');
    } catch (err) {
      const status = err.response?.status;
      const data = err.response?.data;

      if (status === 409) {
        if (data?.code === 'PERIODIC_WO_EXISTS' || data?.message?.includes('kỳ')) {
          setServerError(
            'Thiết bị đã có phiếu bảo trì định kỳ đang mở cho kỳ này. Không thể tạo trùng kỳ (BR-EM-02).'
          );
        } else if (data?.code === 'EQUIPMENT_NOT_ACTIVE') {
          setServerError('Chỉ có thể tạo work order định kỳ cho thiết bị đang ở trạng thái ACTIVE.');
        } else {
          setServerError(data?.message || 'Xung đột trạng thái khi tạo phiếu bảo trì.');
        }
      } else if (status === 422) {
        setServerError(
          data?.message || 'Thiết bị chưa được cấu hình kế hoạch bảo trì định kỳ hợp lệ.'
        );
      } else {
        setServerError(
          data?.message || 'Đã có lỗi xảy ra khi tạo Work Order. Vui lòng kiểm tra lại.'
        );
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <PageHeader
        title="Tạo mới & Phân công Work Order"
        subtitle="Lập phiếu bảo trì định kỳ hoặc phiếu sửa chữa sự cố và điều phối kỹ thuật viên phụ trách"
        breadcrumbs={[
          { label: 'Bàn làm việc', to: '/manager/dashboard' },
          { label: 'Phiếu bảo trì', to: '/manager/work-orders' },
          { label: 'Tạo mới' },
        ]}
      />

      <Row className="justify-content-center">
        <Col xs={12} lg={8}>
          <Card className="border border-light-subtle shadow-sm">
            <Card.Body className="p-4">
              {serverError && (
                <Alert variant="danger" className="d-flex align-items-center gap-2 mb-4">
                  <i className="bi bi-exclamation-octagon-fill fs-5 flex-shrink-0"></i>
                  <div>{serverError}</div>
                </Alert>
              )}

              {loading ? (
                <div className="py-5 text-center">
                  <Spinner animation="border" variant="primary" />
                  <div className="small text-secondary mt-2">Đang nạp danh mục dữ liệu...</div>
                </div>
              ) : (
                <Form onSubmit={handleSubmit}>
                   <Form.Group className="mb-4">
                    <Form.Label className="fw-semibold small d-block">
                      Loại phiếu bảo trì (Work Order Type) <span className="text-danger">*</span>
                    </Form.Label>
                    <div className="d-flex gap-4 mt-2">
                      <Form.Check
                        type="radio"
                        id="type-periodic"
                        name="woType"
                        label={
                          <span>
                            <strong className="text-dark">Bảo trì định kỳ</strong> (PERIODIC) — theo
                            kế hoạch chu kỳ
                          </span>
                        }
                        value="PERIODIC"
                        checked={type === 'PERIODIC'}
                        onChange={(e) => setType(e.target.value)}
                      />
                      <Form.Check
                        type="radio"
                        id="type-repair"
                        name="woType"
                        label={
                          <span>
                            <strong className="text-dark">Sửa chữa sự cố</strong> (REPAIR) — khắc
                            phục hỏng hóc
                          </span>
                        }
                        value="REPAIR"
                        checked={type === 'REPAIR'}
                        onChange={(e) => setType(e.target.value)}
                      />
                    </div>
                  </Form.Group>

                   <Form.Group className="mb-3">
                    <Form.Label className="fw-semibold small">
                      Thiết bị áp dụng <span className="text-danger">*</span>
                    </Form.Label>
                    <Form.Select
                      value={equipmentId}
                      onChange={(e) => setEquipmentId(e.target.value)}
                      required
                    >
                      <option value="">-- Chọn thiết bị trong đơn vị --</option>
                      {equipmentList.map((eq) => (
                        <option
                          key={eq.equipmentId || eq.id}
                          value={eq.equipmentId || eq.id}
                          disabled={type === 'PERIODIC' && eq.status !== 'ACTIVE'}
                        >
                          {eq.code} — {eq.brand || ''} {eq.model || ''} ({eq.type?.name || '—'})
                          {eq.status !== 'ACTIVE' ? ` [${eq.status}]` : ''}
                        </option>
                      ))}
                    </Form.Select>
                    <Form.Text className="text-secondary small">
                      {type === 'PERIODIC'
                        ? 'Phiếu định kỳ chỉ có thể tạo cho thiết bị ở trạng thái ACTIVE.'
                        : 'Phiếu sửa chữa có thể lập cho thiết bị gặp sự cố.'}
                    </Form.Text>
                  </Form.Group>

                   {type === 'PERIODIC' && (
                    <div className="p-3 mb-4 rounded-3 border bg-light-subtle">
                      <div className="d-flex align-items-center gap-2 mb-1">
                        <i className="bi bi-info-circle text-primary"></i>
                        <span className="fw-bold small text-dark">Kỳ bảo trì (Period):</span>
                        {selectedEqPlan?.nextDueDate ? (
                          <Badge bg="primary-subtle" className="text-primary border border-primary-subtle font-monospace">
                            Kỳ: {selectedEqPlan.nextDueDate}
                          </Badge>
                        ) : (
                          <Badge bg="secondary-subtle" className="text-secondary border font-monospace">
                            Server tự động xác định từ Kế hoạch
                          </Badge>
                        )}
                      </div>
                      <small className="text-secondary d-block">
                        Hệ thống tự động xác định kỳ bảo trì từ ngày đến hạn của kế hoạch định kỳ
                        (Next Due Date). Không nhập thủ công để đảm bảo toàn vẹn dữ liệu và chống trùng kỳ.
                      </small>
                    </div>
                  )}

                   {type === 'REPAIR' && (
                    <Form.Group className="mb-4">
                      <Form.Label className="fw-semibold small">
                        Sự cố kỹ thuật liên quan <span className="text-danger">*</span>
                      </Form.Label>
                      <Form.Select
                        value={incidentId}
                        onChange={(e) => setIncidentId(e.target.value)}
                        required
                      >
                        <option value="">-- Chọn sự cố cần khắc phục --</option>
                        {openIncidents.map((inc) => (
                          <option key={inc.incidentId || inc.id} value={inc.incidentId || inc.id}>
                            [{inc.trackingCode}] {inc.description?.substring(0, 60)}... (Thiết bị:{' '}
                            {inc.equipmentCode || '—'})
                          </option>
                        ))}
                      </Form.Select>
                    </Form.Group>
                  )}

                   <Form.Group className="mb-4">
                    <Form.Label className="fw-semibold small">
                      Phân công Kỹ thuật viên (Dropdown chỉ hiển thị TECHNICIAN)
                    </Form.Label>
                    <Form.Select
                      value={technicianId}
                      onChange={(e) => setTechnicianId(e.target.value)}
                    >
                      <option value="">-- Chưa phân công ngay (Để ở trạng thái OPEN) --</option>
                      {technicians.map((t) => (
                        <option key={t.userId || t.id} value={t.userId || t.id}>
                          {t.fullName}{t.username ? ` (@${t.username})` : ''}
                        </option>
                      ))}
                    </Form.Select>
                    <Form.Text className="text-secondary small">
                      Nếu chọn kỹ thuật viên, phiếu sẽ chuyển sang trạng thái ASSIGNED ngay sau khi tạo.
                    </Form.Text>
                  </Form.Group>

                  <div className="d-flex justify-content-end gap-2 pt-3 border-top">
                    <Button
                      variant="outline-secondary"
                      onClick={() => navigate('/manager/work-orders')}
                      disabled={submitting}
                    >
                      Hủy bỏ
                    </Button>
                    <Button
                      type="submit"
                      variant="primary"
                      disabled={submitting}
                      className="d-inline-flex align-items-center gap-2"
                    >
                      {submitting && <Spinner size="sm" animation="border" />}
                      <span>Tạo Work Order</span>
                    </Button>
                  </div>
                </Form>
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </div>
  );
}
