import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useFormik } from 'formik';
import * as Yup from 'yup';
import Card from 'react-bootstrap/Card';
import Form from 'react-bootstrap/Form';
import Button from 'react-bootstrap/Button';
import Alert from 'react-bootstrap/Alert';
import Spinner from 'react-bootstrap/Spinner';
import { equipmentApi, incidentsApi } from '../../api';
import PageHeader from '../../components/common/PageHeader';

export default function IncidentReportPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const prefillEqId = searchParams.get('equipmentId') || '';

  const [equipmentList, setEquipmentList] = useState([]);
  const [loadingEq, setLoadingEq] = useState(true);
  const [serverError, setServerError] = useState(null);
  const [successData, setSuccessData] = useState(null);

  useEffect(() => {
    let isMounted = true;
    const fetchEquipment = async () => {
      setLoadingEq(true);
      try {
        const res = await equipmentApi.getEquipment({ size: 100 });
        if (isMounted) {
          const items = res?.items || res?.data?.items || [];
          setEquipmentList(items);
        }
      } catch (err) {
        if (isMounted) {
          setServerError(err.message || 'Không thể nạp danh sách thiết bị.');
        }
      } finally {
        if (isMounted) setLoadingEq(false);
      }
    };

    fetchEquipment();
    return () => {
      isMounted = false;
    };
  }, []);

  const formik = useFormik({
    initialValues: {
      equipmentId: prefillEqId ? String(prefillEqId) : '',
      description: '',
    },
    enableReinitialize: true,
    validationSchema: Yup.object({
      equipmentId: Yup.number().required('Vui lòng chọn thiết bị gặp sự cố.'),
      description: Yup.string()
        .trim()
        .min(10, 'Mô tả sự cố phải có ít nhất 10 ký tự.')
        .max(1000, 'Mô tả sự cố không vượt quá 1000 ký tự.')
        .required('Vui lòng nhập mô tả chi tiết sự cố.'),
    }),
    onSubmit: async (values, { setSubmitting }) => {
      setServerError(null);
      setSuccessData(null);
      try {
        const payload = {
          equipmentId: Number(values.equipmentId),
          description: values.description.trim(),
        };
        const res = await incidentsApi.createIncident(payload);
        const created = res?.data || res;
        setSuccessData(created);
      } catch (err) {
        setServerError(err.message || 'Không thể gửi báo cáo sự cố. Vui lòng thử lại.');
      } finally {
        setSubmitting(false);
      }
    },
  });

  return (
    <div>
      <PageHeader
        title="Báo cáo Sự cố Thiết bị"
        subtitle="Gửi thông báo hỏng hóc hoặc trục trặc kỹ thuật tới ban quản lý và đội kỹ thuật"
      />

      {successData ? (
        <Card className="border-0 shadow-sm border-start border-4 border-success p-4 mb-4">
          <Card.Body className="text-center py-4">
            <div
              className="rounded-circle d-inline-flex align-items-center justify-content-center mb-3"
              style={{ width: '64px', height: '64px', backgroundColor: '#DCFCE7', color: '#15803D' }}
            >
              <i className="bi bi-check-circle-fill fs-2"></i>
            </div>
            <h2 className="h4 fw-bold text-dark mb-2">Báo cáo sự cố đã được ghi nhận thành công!</h2>
            <p className="text-secondary mb-3">
              Mã theo dõi sự cố:{' '}
              <span className="font-monospace fw-bold text-primary fs-5">
                {successData.trackingCode || `INC-${successData.incidentId}`}
              </span>
            </p>
            <p className="text-muted small max-w-lg mx-auto mb-4">
              Hệ thống đã chuyển thông tin tới Ban quản lý để thẩm định mức độ nghiêm trọng và phân công kỹ thuật viên xử lý.
            </p>
            <div className="d-flex justify-content-center gap-2">
              <Button variant="primary" onClick={() => navigate('/user/incidents')}>
                Xem danh sách sự cố của tôi
              </Button>
              <Button
                variant="outline-secondary"
                onClick={() => {
                  setSuccessData(null);
                  formik.resetForm();
                }}
              >
                Báo cáo sự cố khác
              </Button>
            </div>
          </Card.Body>
        </Card>
      ) : (
        <Card className="border border-light-subtle shadow-sm p-3">
          <Card.Body>
            {serverError && (
              <Alert variant="danger" dismissible onClose={() => setServerError(null)} className="mb-4">
                <i className="bi bi-exclamation-triangle-fill me-2"></i>
                {serverError}
              </Alert>
            )}

            <Form onSubmit={formik.handleSubmit}>
              <Form.Group className="mb-3" controlId="equipmentId">
                <Form.Label className="fw-semibold">
                  Thiết bị gặp sự cố <span className="text-danger">*</span>
                </Form.Label>
                {loadingEq ? (
                  <div className="d-flex align-items-center gap-2 py-2 text-muted small">
                    <Spinner size="sm" animation="border" />
                    <span>Đang nạp danh sách thiết bị...</span>
                  </div>
                ) : (
                  <Form.Select
                    name="equipmentId"
                    value={formik.values.equipmentId}
                    onChange={formik.handleChange}
                    onBlur={formik.handleBlur}
                    isInvalid={formik.touched.equipmentId && Boolean(formik.errors.equipmentId)}
                  >
                    <option value="">-- Chọn thiết bị trong phòng lab --</option>
                    {equipmentList.map((eq) => (
                      <option key={eq.equipmentId || eq.id} value={eq.equipmentId || eq.id}>
                        {eq.code} — {eq.brand} {eq.model} ({eq.location?.name || 'Vị trí N/A'})
                      </option>
                    ))}
                  </Form.Select>
                )}
                <Form.Control.Feedback type="invalid">
                  {formik.errors.equipmentId}
                </Form.Control.Feedback>
              </Form.Group>

              <Form.Group className="mb-4" controlId="description">
                <Form.Label className="fw-semibold">
                  Mô tả hiện tượng / Chi tiết hư hỏng <span className="text-danger">*</span>
                </Form.Label>
                <Form.Control
                  as="textarea"
                  rows={4}
                  placeholder="Mô tả cụ thể hiện tượng hư hỏng, mã lỗi hiển thị (nếu có), hoặc tiếng động bất thường..."
                  name="description"
                  value={formik.values.description}
                  onChange={formik.handleChange}
                  onBlur={formik.handleBlur}
                  isInvalid={formik.touched.description && Boolean(formik.errors.description)}
                />
                <Form.Control.Feedback type="invalid">
                  {formik.errors.description}
                </Form.Control.Feedback>
                <Form.Text className="text-muted small">
                  Tối thiểu 10 ký tự. Vui lòng cung cấp thông tin chi tiết nhất có thể để kỹ thuật viên chuẩn bị phụ tùng.
                </Form.Text>
              </Form.Group>

              <div className="d-flex justify-content-end gap-2 border-top pt-3">
                <Button variant="outline-secondary" onClick={() => navigate(-1)} disabled={formik.isSubmitting}>
                  Hủy bỏ
                </Button>
                <Button
                  variant="primary"
                  type="submit"
                  disabled={formik.isSubmitting || loadingEq}
                  className="d-flex align-items-center gap-2"
                >
                  {formik.isSubmitting && <Spinner size="sm" animation="border" />}
                  <span>Gửi báo cáo sự cố</span>
                </Button>
              </div>
            </Form>
          </Card.Body>
        </Card>
      )}
    </div>
  );
}

