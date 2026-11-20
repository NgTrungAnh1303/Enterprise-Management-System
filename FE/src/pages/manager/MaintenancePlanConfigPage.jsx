import React, { useState, useEffect, useCallback } from 'react';
import Card from 'react-bootstrap/Card';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import Tabs from 'react-bootstrap/Tabs';
import Tab from 'react-bootstrap/Tab';
import Alert from 'react-bootstrap/Alert';
import Spinner from 'react-bootstrap/Spinner';
import Badge from 'react-bootstrap/Badge';
import { equipmentApi, equipmentTypesApi } from '../../api';
import PageHeader from '../../components/common/PageHeader';

export default function MaintenancePlanConfigPage() {
  const [activeTab, setActiveTab] = useState('plan');

  
  const [equipmentList, setEquipmentList] = useState([]);
  const [types, setTypes] = useState([]);


  const [selectedEqId, setSelectedEqId] = useState('');
  const [currentPlan, setCurrentPlan] = useState(null);
  const [planCycle, setPlanCycle] = useState('90');
  const [planStartDate, setPlanStartDate] = useState(
    new Date().toISOString().split('T')[0]
  );
  const [planLoading, setPlanLoading] = useState(false);
  const [planSaving, setPlanSaving] = useState(false);
  const [planError, setPlanError] = useState(null);
  const [planSuccess, setPlanSuccess] = useState(null);

  
  const [selectedTypeId, setSelectedTypeId] = useState('');
  const [checklistItems, setChecklistItems] = useState([]);
  const [newItemText, setNewItemText] = useState('');
  const [newItemRequired, setNewItemRequired] = useState(true);
  const [checklistLoading, setChecklistLoading] = useState(false);
  const [checklistSaving, setChecklistSaving] = useState(false);
  const [checklistError, setChecklistError] = useState(null);
  const [checklistSuccess, setChecklistSuccess] = useState(null);

  
  useEffect(() => {
    let isMounted = true;
    const loadMetadata = async () => {
      try {
        const [eqRes, typesRes] = await Promise.all([
          equipmentApi.getEquipment({ size: 100 }),
          equipmentTypesApi.getEquipmentTypes(),
        ]);
        if (isMounted) {
          const eqItems = eqRes?.items || eqRes?.data?.items || [];
          setEquipmentList(eqItems);
          if (eqItems.length > 0) {
            setSelectedEqId(String(eqItems[0].equipmentId || eqItems[0].id));
          }

          const typeItems = Array.isArray(typesRes) ? typesRes : (typesRes?.items || typesRes?.data || []);
          setTypes(typeItems);
          if (typeItems.length > 0) {
            setSelectedTypeId(String(typeItems[0].typeId || typeItems[0].id));
          }
        }
      } catch (err) {
        console.warn('Không thể nạp danh mục cấu hình:', err);
      }
    };
    loadMetadata();
    return () => {
      isMounted = false;
    };
  }, []);

  
  const fetchEquipmentPlan = useCallback(async (eqId) => {
    if (!eqId) return;
    setPlanLoading(true);
    setPlanError(null);
    setPlanSuccess(null);
    try {
      const res = await equipmentApi.getEquipmentPlan(eqId);
      const plan = res?.data || res;
      setCurrentPlan(plan);
      if (plan) {
        setPlanCycle(String(plan.cycleDays || 90));
      }
    } catch (err) {
      if (err.response?.status === 404) {
        setCurrentPlan(null);
      } else {
        setPlanError(err.response?.data?.message || 'Không thể nạp kế hoạch thiết bị.');
      }
    } finally {
      setPlanLoading(false);
    }
  }, []);

  useEffect(() => {
    if (selectedEqId) {
      fetchEquipmentPlan(selectedEqId);
    }
  }, [selectedEqId, fetchEquipmentPlan]);

  
  const fetchTypeChecklist = useCallback(async (tId) => {
    if (!tId) return;
    setChecklistLoading(true);
    setChecklistError(null);
    setChecklistSuccess(null);
    try {
      const res = await equipmentTypesApi.getTypeChecklist(tId);
      setChecklistItems(Array.isArray(res) ? res : (res?.items || res?.data || []));
    } catch (err) {
      setChecklistError(err.response?.data?.message || 'Không thể nạp danh mục checklist.');
    } finally {
      setChecklistLoading(false);
    }
  }, []);

  useEffect(() => {
    if (selectedTypeId) {
      fetchTypeChecklist(selectedTypeId);
    }
  }, [selectedTypeId, fetchTypeChecklist]);

  
  const handleSavePlan = async (e) => {
    e.preventDefault();
    if (!selectedEqId) return;
    setPlanSaving(true);
    setPlanError(null);
    setPlanSuccess(null);
    try {
      const cycle = Number(planCycle);
      if (![30, 90, 180].includes(cycle)) {
        setPlanError('Chu kỳ bảo trì chỉ chấp nhận giá trị 30, 90 hoặc 180 ngày.');
        setPlanSaving(false);
        return;
      }
      await equipmentApi.updateEquipmentPlan(selectedEqId, {
        cycleDays: cycle,
        startDate: planStartDate,
      });
      setPlanSuccess('Cập nhật kế hoạch chu kỳ bảo dưỡng thành công!');
      await fetchEquipmentPlan(selectedEqId);
    } catch (err) {
      setPlanError(err.response?.data?.message || 'Đã có lỗi xảy ra khi lưu kế hoạch.');
    } finally {
      setPlanSaving(false);
    }
  };

 
  const handleAddChecklistItem = (e) => {
    e.preventDefault();
    if (!newItemText.trim()) return;
    const newItem = {
      checklistId: Date.now(), 
      item: newItemText.trim(),
      isRequired: newItemRequired,
    };
    setChecklistItems((prev) => [...prev, newItem]);
    setNewItemText('');
    setNewItemRequired(true);
  };

  
  const handleRemoveChecklistItem = (idx) => {
    setChecklistItems((prev) => prev.filter((_, i) => i !== idx));
  };

  
  const handleToggleRequired = (idx) => {
    setChecklistItems((prev) =>
      prev.map((item, i) =>
        i === idx ? { ...item, isRequired: !item.isRequired } : item
      )
    );
  };

  
  const handleSaveChecklist = async () => {
    if (!selectedTypeId) return;
    if (checklistItems.length === 0) {
      setChecklistError('Danh mục checklist không được để trống.');
      return;
    }
    setChecklistSaving(true);
    setChecklistError(null);
    setChecklistSuccess(null);
    try {
      const payload = checklistItems.map((item) => ({
        item: item.item.trim(),
        isRequired: Boolean(item.isRequired),
      }));
      await equipmentTypesApi.updateTypeChecklist(selectedTypeId, { items: payload });
      setChecklistSuccess('Cập nhật danh mục checklist kiểm tra thành công!');
      await fetchTypeChecklist(selectedTypeId);
    } catch (err) {
      setChecklistError(
        err.response?.data?.message || 'Không thể lưu danh mục checklist. Vui lòng thử lại.'
      );
    } finally {
      setChecklistSaving(false);
    }
  };

  return (
    <div>
      <PageHeader
        title="Cấu hình Chu kỳ & Checklist Bảo trì"
        subtitle="Thiết lập chu kỳ định kỳ 30/90/180 ngày cho thiết bị và quy chuẩn danh mục kiểm tra theo từng chủng loại"
        breadcrumbs={[
          { label: 'Bàn làm việc', to: '/manager/dashboard' },
          { label: 'Cấu hình kế hoạch' },
        ]}
      />

      <Card className="border border-light-subtle shadow-sm">
        <Card.Body className="p-3">
          <Tabs activeKey={activeTab} onSelect={(k) => setActiveTab(k)} className="mb-4">
            {/* TAB 1: EQUIPMENT MAINTENANCE PLAN */}
            <Tab
              eventKey="plan"
              title={
                <span className="d-inline-flex align-items-center gap-2">
                  <i className="bi bi-clock-history"></i>
                  <span>Chu kỳ bảo trì thiết bị (30/90/180 ngày)</span>
                </span>
              }
            >
              <Row className="g-4">
                <Col xs={12} md={5}>
                  <Card className="border border-light-subtle bg-light-subtle">
                    <Card.Body className="p-3">
                      <Form.Group className="mb-3">
                        <Form.Label className="small fw-semibold">Chọn thiết bị</Form.Label>
                        <Form.Select
                          value={selectedEqId}
                          onChange={(e) => setSelectedEqId(e.target.value)}
                        >
                          {equipmentList.map((eq) => (
                            <option
                              key={eq.equipmentId || eq.id}
                              value={eq.equipmentId || eq.id}
                            >
                              {eq.code} - {eq.brand || ''} {eq.model || ''} ({eq.type?.name || '—'})
                            </option>
                          ))}
                        </Form.Select>
                      </Form.Group>

                      {planLoading ? (
                        <div className="py-4 text-center">
                          <Spinner size="sm" animation="border" />
                          <div className="small text-secondary mt-1">Đang tải kế hoạch...</div>
                        </div>
                      ) : currentPlan ? (
                        <div className="p-3 bg-white rounded border small">
                          <div className="fw-semibold text-dark mb-2 border-bottom pb-2">
                            Kế hoạch hiện tại
                          </div>
                          <div className="mb-1">
                            <span className="text-secondary">Chu kỳ:</span>{' '}
                            <strong>{currentPlan.cycleDays} ngày/lần</strong>
                          </div>
                          <div className="mb-1">
                            <span className="text-secondary">Hoàn tất gần nhất:</span>{' '}
                            <span className="font-monospace">
                              {currentPlan.lastCompletedDate || 'Chưa có'}
                            </span>
                          </div>
                          <div>
                            <span className="text-secondary">Hạn tiếp theo:</span>{' '}
                            <span className="fw-bold text-primary font-monospace">
                              {currentPlan.nextDueDate || 'Chưa tính'}
                            </span>
                          </div>
                        </div>
                      ) : (
                        <div className="p-3 bg-white rounded border text-center text-secondary small">
                          Thiết bị chưa có kế hoạch định kỳ. Hãy thiết lập chu kỳ bên phải.
                        </div>
                      )}
                    </Card.Body>
                  </Card>
                </Col>

                <Col xs={12} md={7}>
                  <Card className="border border-light-subtle">
                    <Card.Body className="p-4">
                      <h6 className="fw-bold text-dark mb-3">Thiết lập / Cập nhật chu kỳ</h6>

                      {planError && (
                        <Alert variant="danger" className="py-2 small mb-3">
                          {planError}
                        </Alert>
                      )}
                      {planSuccess && (
                        <Alert variant="success" className="py-2 small mb-3">
                          {planSuccess}
                        </Alert>
                      )}

                      <Form onSubmit={handleSavePlan}>
                        <Form.Group className="mb-4">
                          <Form.Label className="small fw-semibold d-block">
                            Chọn chu kỳ bảo trì định kỳ <span className="text-danger">*</span>
                          </Form.Label>
                          <div className="d-flex flex-column gap-2 mt-2">
                            <Form.Check
                              type="radio"
                              id="cycle-30"
                              name="planCycle"
                              label="30 ngày (Bảo dưỡng hàng tháng)"
                              value="30"
                              checked={planCycle === '30'}
                              onChange={(e) => setPlanCycle(e.target.value)}
                            />
                            <Form.Check
                              type="radio"
                              id="cycle-90"
                              name="planCycle"
                              label="90 ngày (Bảo dưỡng hàng quý - Tiêu chuẩn)"
                              value="90"
                              checked={planCycle === '90'}
                              onChange={(e) => setPlanCycle(e.target.value)}
                            />
                            <Form.Check
                              type="radio"
                              id="cycle-180"
                              name="planCycle"
                              label="180 ngày (Bảo dưỡng nửa năm / Định kỳ lớn)"
                              value="180"
                              checked={planCycle === '180'}
                              onChange={(e) => setPlanCycle(e.target.value)}
                            />
                          </div>
                          <Form.Text className="text-secondary small">
                            Hệ thống chỉ chấp nhận một trong 3 mốc chu kỳ theo đặc tả: 30, 90 hoặc 180 ngày.
                          </Form.Text>
                        </Form.Group>

                        <Form.Group className="mb-4">
                          <Form.Label className="small fw-semibold">
                            Mốc ngày bắt đầu tính chu kỳ <span className="text-danger">*</span>
                          </Form.Label>
                          <Form.Control
                            type="date"
                            value={planStartDate}
                            onChange={(e) => setPlanStartDate(e.target.value)}
                            required
                            style={{ maxWidth: '240px' }}
                          />
                          <Form.Text className="text-secondary small">
                            Hệ thống sẽ tự động tính hạn bảo trì tiếp theo (next_due_date = mốc + chu kỳ).
                          </Form.Text>
                        </Form.Group>

                        <Button
                          type="submit"
                          variant="primary"
                          disabled={planSaving}
                          className="d-inline-flex align-items-center gap-2"
                        >
                          {planSaving && <Spinner size="sm" animation="border" />}
                          <span>Lưu cấu hình kế hoạch</span>
                        </Button>
                      </Form>
                    </Card.Body>
                  </Card>
                </Col>
              </Row>
            </Tab>

            
            <Tab
              eventKey="checklist"
              title={
                <span className="d-inline-flex align-items-center gap-2">
                  <i className="bi bi-card-checklist"></i>
                  <span>Danh mục Checklist theo Loại thiết bị</span>
                </span>
              }
            >
              <div className="mb-3 d-flex align-items-center gap-3">
                <Form.Label className="small fw-semibold mb-0 text-nowrap">
                  Chọn loại thiết bị:
                </Form.Label>
                <Form.Select
                  value={selectedTypeId}
                  onChange={(e) => setSelectedTypeId(e.target.value)}
                  style={{ maxWidth: '320px' }}
                >
                  {types.map((t) => (
                    <option key={t.typeId || t.id} value={t.typeId || t.id}>
                      {t.name}
                    </option>
                  ))}
                </Form.Select>
              </div>

              {checklistError && (
                <Alert variant="danger" className="py-2 small mb-3">
                  {checklistError}
                </Alert>
              )}
              {checklistSuccess && (
                <Alert variant="success" className="py-2 small mb-3">
                  {checklistSuccess}
                </Alert>
              )}

              <Card className="border border-light-subtle mb-4">
                <Card.Header className="bg-light-subtle py-2 d-flex justify-content-between align-items-center">
                  <span className="small fw-bold text-dark">
                    Danh sách các hạng mục kiểm tra ({checklistItems.length})
                  </span>
                  <span className="small text-secondary">
                    Hạng mục đánh dấu bắt buộc sẽ được kiểm soát khi nộp Work Order
                  </span>
                </Card.Header>
                <Card.Body className="p-0">
                  {checklistLoading ? (
                    <div className="py-5 text-center">
                      <Spinner size="sm" animation="border" />
                      <div className="small text-secondary mt-1">Đang tải checklist...</div>
                    </div>
                  ) : checklistItems.length > 0 ? (
                    <div className="table-responsive">
                      <table className="table table-hover align-middle mb-0" style={{ fontSize: '0.875rem' }}>
                        <thead className="table-light">
                          <tr>
                            <th style={{ width: '60px' }}>STT</th>
                            <th>Nội dung hạng mục kiểm tra</th>
                            <th style={{ width: '160px' }} className="text-center">
                              Bắt buộc (Required)
                            </th>
                            <th style={{ width: '90px' }} className="text-end">
                              Thao tác
                            </th>
                          </tr>
                        </thead>
                        <tbody>
                          {checklistItems.map((item, idx) => (
                            <tr key={item.checklistId || idx}>
                              <td className="text-secondary">{idx + 1}</td>
                              <td>
                                <span className="fw-medium text-dark">{item.item}</span>
                              </td>
                              <td className="text-center">
                                <Form.Check
                                  type="checkbox"
                                  id={`req-${idx}`}
                                  checked={Boolean(item.isRequired)}
                                  onChange={() => handleToggleRequired(idx)}
                                  label={
                                    item.isRequired ? (
                                      <Badge bg="danger-subtle" className="text-danger border border-danger-subtle">
                                        Bắt buộc
                                      </Badge>
                                    ) : (
                                      <Badge bg="secondary-subtle" className="text-secondary border">
                                        Tùy chọn
                                      </Badge>
                                    )
                                  }
                                />
                              </td>
                              <td className="text-end">
                                <Button
                                  variant="outline-danger"
                                  size="sm"
                                  onClick={() => handleRemoveChecklistItem(idx)}
                                  title="Xóa hạng mục này"
                                >
                                  <i className="bi bi-trash"></i>
                                </Button>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  ) : (
                    <div className="p-4 text-center text-secondary small">
                      Chưa có hạng mục kiểm tra nào cho loại thiết bị này. Hãy thêm mới bên dưới.
                    </div>
                  )}
                </Card.Body>
              </Card>

              
              <Card className="border border-light-subtle mb-3">
                <Card.Body className="p-3">
                  <h6 className="small fw-bold text-dark mb-3">Thêm hạng mục kiểm tra mới</h6>
                  <Form onSubmit={handleAddChecklistItem}>
                    <Row className="g-2 align-items-center">
                      <Col xs={12} md={7}>
                        <Form.Control
                          type="text"
                          placeholder="Nhập nội dung hạng mục (ví dụ: Kiểm tra nguồn điện, Vệ sinh buồng mẫu...)"
                          value={newItemText}
                          onChange={(e) => setNewItemText(e.target.value)}
                        />
                      </Col>
                      <Col xs={6} md={3}>
                        <Form.Check
                          type="checkbox"
                          id="new-item-req"
                          label="Đánh dấu bắt buộc"
                          checked={newItemRequired}
                          onChange={(e) => setNewItemRequired(e.target.checked)}
                        />
                      </Col>
                      <Col xs={6} md={2} className="text-end">
                        <Button
                          type="submit"
                          variant="outline-primary"
                          className="w-100 d-inline-flex align-items-center justify-content-center gap-1"
                          disabled={!newItemText.trim()}
                        >
                          <i className="bi bi-plus-lg"></i>
                          <span>Thêm</span>
                        </Button>
                      </Col>
                    </Row>
                  </Form>
                </Card.Body>
              </Card>

             
              <div className="d-flex justify-content-end">
                <Button
                  variant="primary"
                  onClick={handleSaveChecklist}
                  disabled={checklistSaving || checklistItems.length === 0}
                  className="d-inline-flex align-items-center gap-2"
                >
                  {checklistSaving && <Spinner size="sm" animation="border" />}
                  <span>Lưu toàn bộ danh mục Checklist</span>
                </Button>
              </div>
            </Tab>
          </Tabs>
        </Card.Body>
      </Card>
    </div>
  );
}

