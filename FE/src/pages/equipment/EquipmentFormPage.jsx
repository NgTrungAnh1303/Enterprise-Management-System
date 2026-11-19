import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Formik, Form as FormikForm } from 'formik';
import * as Yup from 'yup';
import Card from 'react-bootstrap/Card';
import Button from 'react-bootstrap/Button';
import Spinner from 'react-bootstrap/Spinner';
import Alert from 'react-bootstrap/Alert';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import { useAuth } from '../../auth/AuthContext';
import { equipmentApi, equipmentTypesApi, locationsApi } from '../../api';
import PageHeader from '../../components/common/PageHeader';
import FormField from '../../components/common/FormField';
import LoadingState from '../../components/common/LoadingState';

const validationSchema = Yup.object().shape({
  code: Yup.string()
    .trim()
    .required('Mã thiết bị không được để trống')
    .max(50, 'Mã thiết bị tối đa 50 ký tự'),
  typeId: Yup.string().required('Vui lòng chọn loại thiết bị'),
  locationId: Yup.string().required('Vui lòng chọn vị trí lắp đặt'),
  brand: Yup.string().trim().max(100, 'Hãng sản xuất tối đa 100 ký tự'),
  model: Yup.string().trim().max(100, 'Model tối đa 100 ký tự'),
  serial: Yup.string().trim().max(100, 'Số serial tối đa 100 ký tự'),
});

export default function EquipmentFormPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { role } = useAuth();
  const isEdit = Boolean(id);

  const [initialValues, setInitialValues] = useState({
    code: '',
    typeId: '',
    locationId: '',
    brand: '',
    model: '',
    serial: '',
  });

  const [loading, setLoading] = useState(isEdit);
  const [types, setTypes] = useState([]);
  const [locations, setLocations] = useState([]);
  const [serverError, setServerError] = useState(null);

  const basePath = role === 'ADMIN' ? '/admin' : '/manager';

  // Load dropdown lists and existing equipment if editing
  useEffect(() => {
    let isMounted = true;
    const loadData = async () => {
      try {
        const [typesRes, locsRes] = await Promise.all([
          equipmentTypesApi.getEquipmentTypes(),
          locationsApi.getLocations(),
        ]);
        if (isMounted) {
          setTypes(Array.isArray(typesRes) ? typesRes : (typesRes?.items || typesRes?.data || []));
          setLocations(Array.isArray(locsRes) ? locsRes : (locsRes?.items || locsRes?.data || []));
        }

        if (isEdit) {
          const eqRes = await equipmentApi.getEquipmentById(id);
          const eq = eqRes?.data || eqRes || {};
          if (isMounted) {
            setInitialValues({
              code: eq.code || '',
              typeId: eq.type?.typeId ? String(eq.type.typeId) : '',
              locationId: eq.location?.locationId ? String(eq.location.locationId) : '',
              brand: eq.brand || '',
              model: eq.model || '',
              serial: eq.serial || '',
            });
          }
        }
      } catch (err) {
        if (isMounted) {
          setServerError(
            err.response?.data?.message || 'Không thể nạp thông tin thiết bị từ máy chủ.'
          );
        }
      } finally {
        if (isMounted) setLoading(false);
      }
    };
    loadData();
    return () => {
      isMounted = false;
    };
  }, [id, isEdit]);

  const handleSubmit = async (values, { setSubmitting }) => {
    setServerError(null);
    try {
      const payload = {
        typeId: Number(values.typeId),
        locationId: Number(values.locationId),
        brand: values.brand?.trim() || null,
        model: values.model?.trim() || null,
        serial: values.serial?.trim() || null,
      };

      if (!isEdit) {
        payload.code = values.code.trim();
        await equipmentApi.createEquipment(payload);
      } else {
        await equipmentApi.updateEquipment(id, payload);
      }

      // On success, redirect back to list
      navigate(role === 'ADMIN' ? '/admin/equipment-catalog' : '/manager/equipment');
    } catch (err) {
      const status = err.response?.status;
      const data = err.response?.data;
      if (status === 409) {
        setServerError('Mã thiết bị đã tồn tại trong hệ thống. Vui lòng chọn mã khác.');
      } else if (status === 400) {
        setServerError(data?.message || 'Thông tin nhập liệu không hợp lệ. Vui lòng kiểm tra lại.');
      } else {
        setServerError(data?.message || 'Đã có lỗi xảy ra khi lưu thiết bị. Vui lòng thử lại.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="p-5 text-center bg-white rounded-3 border">
        <LoadingState message="Đang nạp dữ liệu thiết bị..." />
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title={isEdit ? 'Cập nhật thông tin thiết bị' : 'Thêm thiết bị mới'}
        subtitle={
          isEdit
            ? `Chỉnh sửa thông số kỹ thuật thiết bị ${initialValues.code} (mã thiết bị không được thay đổi)`
            : 'Đăng ký thiết bị phòng thí nghiệm mới vào hệ thống quản lý'
        }
        breadcrumbs={[
          { label: 'Bàn làm việc', to: `${basePath}/dashboard` },
          {
            label: 'Danh mục thiết bị',
            to: role === 'ADMIN' ? '/admin/equipment-catalog' : '/manager/equipment',
          },
          { label: isEdit ? 'Chỉnh sửa' : 'Thêm mới' },
        ]}
      />

      <Row className="justify-content-center">
        <Col xs={12} lg={8}>
          <Card className="border border-light-subtle shadow-sm">
            <Card.Body className="p-4">
              {serverError && (
                <Alert variant="danger" className="d-flex align-items-center gap-2 mb-4">
                  <i className="bi bi-exclamation-octagon-fill fs-5"></i>
                  <div>{serverError}</div>
                </Alert>
              )}

              <Formik
                initialValues={initialValues}
                validationSchema={validationSchema}
                onSubmit={handleSubmit}
                enableReinitialize
              >
                {({ isSubmitting }) => (
                  <FormikForm noValidate>
                    <Row className="g-3">
                      <Col xs={12} md={6}>
                        <FormField
                          name="code"
                          label="Mã thiết bị"
                          placeholder="Ví dụ: EQ01, BIO-CENT-01"
                          required
                          disabled={isEdit}
                          helpText={
                            isEdit
                              ? 'Mã thiết bị được bảo toàn tính toàn vẹn (không thể thay đổi).'
                              : 'Mã định danh duy nhất của thiết bị.'
                          }
                        />
                      </Col>

                      <Col xs={12} md={6}>
                        <FormField
                          name="typeId"
                          label="Loại thiết bị"
                          as="select"
                          required
                        >
                          <option value="">-- Chọn loại thiết bị --</option>
                          {types.map((t) => (
                            <option key={t.typeId || t.id} value={t.typeId || t.id}>
                              {t.name}
                            </option>
                          ))}
                        </FormField>
                      </Col>

                      <Col xs={12} md={6}>
                        <FormField
                          name="locationId"
                          label="Vị trí lắp đặt"
                          as="select"
                          required
                        >
                          <option value="">-- Chọn vị trí lắp đặt --</option>
                          {locations.map((loc) => (
                            <option key={loc.locationId || loc.id} value={loc.locationId || loc.id}>
                              {loc.room} {loc.area ? `- ${loc.area}` : ''}{' '}
                              {loc.department ? `(${loc.department})` : ''}
                            </option>
                          ))}
                        </FormField>
                      </Col>

                      <Col xs={12} md={6}>
                        <FormField
                          name="brand"
                          label="Hãng sản xuất"
                          placeholder="Ví dụ: Eppendorf, Olympus, Thermo"
                        />
                      </Col>

                      <Col xs={12} md={6}>
                        <FormField
                          name="model"
                          label="Model"
                          placeholder="Ví dụ: 5810R, CX23"
                        />
                      </Col>

                      <Col xs={12} md={6}>
                        <FormField
                          name="serial"
                          label="Số serial"
                          placeholder="Ví dụ: SN-EP-5810-01"
                        />
                      </Col>
                    </Row>

                    <div className="d-flex justify-content-end gap-2 mt-4 pt-3 border-top">
                      <Button
                        variant="outline-secondary"
                        onClick={() =>
                          navigate(
                            role === 'ADMIN' ? '/admin/equipment-catalog' : '/manager/equipment'
                          )
                        }
                        disabled={isSubmitting}
                      >
                        Hủy bỏ
                      </Button>
                      <Button
                        type="submit"
                        variant="primary"
                        disabled={isSubmitting}
                        className="d-inline-flex align-items-center gap-2"
                      >
                        {isSubmitting && <Spinner size="sm" animation="border" />}
                        <span>{isEdit ? 'Cập nhật thiết bị' : 'Lưu thiết bị'}</span>
                      </Button>
                    </div>
                  </FormikForm>
                )}
              </Formik>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </div>
  );
}

