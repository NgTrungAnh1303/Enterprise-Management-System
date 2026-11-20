import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import Tabs from 'react-bootstrap/Tabs';
import Tab from 'react-bootstrap/Tab';
import Card from 'react-bootstrap/Card';
import Button from 'react-bootstrap/Button';
import Modal from 'react-bootstrap/Modal';
import Form from 'react-bootstrap/Form';
import Alert from 'react-bootstrap/Alert';
import Spinner from 'react-bootstrap/Spinner';
import { useAuth } from '../../auth/AuthContext';
import { equipmentTypesApi, locationsApi } from '../../api';
import PageHeader from '../../components/common/PageHeader';
import DataTable from '../../components/common/DataTable';

export default function MasterDataPage() {
  const { role } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = searchParams.get('tab') === 'locations' ? 'locations' : 'types';

  const basePath = role === 'ADMIN' ? '/admin' : '/manager';

  // State Types
  const [types, setTypes] = useState([]);
  const [typesLoading, setTypesLoading] = useState(true);
  const [typesError, setTypesError] = useState(null);

  // State Locations
  const [locations, setLocations] = useState([]);
  const [locsLoading, setLocsLoading] = useState(true);
  const [locsError, setLocsError] = useState(null);

  // Modal State for Types
  const [showTypeModal, setShowTypeModal] = useState(false);
  const [editingType, setEditingType] = useState(null);
  const [typeName, setTypeName] = useState('');
  const [typeDesc, setTypeDesc] = useState('');
  const [typeSubmitting, setTypeSubmitting] = useState(false);
  const [typeModalError, setTypeModalError] = useState(null);

  // Modal State for Locations
  const [showLocModal, setShowLocModal] = useState(false);
  const [editingLoc, setEditingLoc] = useState(null);
  const [room, setRoom] = useState('');
  const [area, setArea] = useState('');
  const [department, setDepartment] = useState('');
  const [locSubmitting, setLocSubmitting] = useState(false);
  const [locModalError, setLocModalError] = useState(null);

  // Load types
  const fetchTypes = useCallback(async () => {
    setTypesLoading(true);
    setTypesError(null);
    try {
      const res = await equipmentTypesApi.getEquipmentTypes();
      setTypes(Array.isArray(res) ? res : (res?.items || res?.data || []));
    } catch (err) {
      setTypesError(err);
    } finally {
      setTypesLoading(false);
    }
  }, []);

  // Load locations
  const fetchLocations = useCallback(async () => {
    setLocsLoading(true);
    setLocsError(null);
    try {
      const res = await locationsApi.getLocations();
      setLocations(Array.isArray(res) ? res : (res?.items || res?.data || []));
    } catch (err) {
      setLocsError(err);
    } finally {
      setLocsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchTypes();
    fetchLocations();
  }, [fetchTypes, fetchLocations]);

  const handleTabSelect = (k) => {
    setSearchParams({ tab: k });
  };

  // Open Type Modal
  const handleOpenTypeModal = (type = null) => {
    setEditingType(type);
    setTypeName(type ? type.name : '');
    setTypeDesc(type ? type.description || '' : '');
    setTypeModalError(null);
    setShowTypeModal(true);
  };

  // Save Type
  const handleSaveType = async (e) => {
    e.preventDefault();
    if (!typeName.trim()) {
      setTypeModalError('Tên loại thiết bị không được để trống.');
      return;
    }
    setTypeSubmitting(true);
    setTypeModalError(null);
    try {
      const payload = {
        name: typeName.trim(),
        description: typeDesc.trim() || null,
      };
      if (editingType) {
        await equipmentTypesApi.updateEquipmentType(editingType.typeId || editingType.id, payload);
      } else {
        await equipmentTypesApi.createEquipmentType(payload);
      }
      setShowTypeModal(false);
      await fetchTypes();
    } catch (err) {
      if (err.response?.status === 409) {
        setTypeModalError('Tên loại thiết bị đã tồn tại trong hệ thống.');
      } else {
        setTypeModalError(err.response?.data?.message || 'Không thể lưu loại thiết bị.');
      }
    } finally {
      setTypeSubmitting(false);
    }
  };

  // Open Location Modal
  const handleOpenLocModal = (loc = null) => {
    setEditingLoc(loc);
    setRoom(loc ? loc.room : '');
    setArea(loc ? loc.area : '');
    setDepartment(loc ? loc.department || '' : '');
    setLocModalError(null);
    setShowLocModal(true);
  };

  // Save Location
  const handleSaveLocation = async (e) => {
    e.preventDefault();
    if (!room.trim() || !area.trim()) {
      setLocModalError('Phòng (Room) và Khu vực (Area) là bắt buộc.');
      return;
    }
    setLocSubmitting(true);
    setLocModalError(null);
    try {
      const payload = {
        room: room.trim(),
        area: area.trim(),
        department: department.trim() || null,
      };
      if (editingLoc) {
        await locationsApi.updateLocation(editingLoc.locationId || editingLoc.id, payload);
      } else {
        await locationsApi.createLocation(payload);
      }
      setShowLocModal(false);
      await fetchLocations();
    } catch (err) {
      setLocModalError(err.response?.data?.message || 'Không thể lưu vị trí lắp đặt.');
    } finally {
      setLocSubmitting(false);
    }
  };

  // Type columns
  const typeColumns = [
    {
      header: 'ID',
      field: 'typeId',
      width: '80px',
      render: (item) => item.typeId || item.id,
    },
    {
      header: 'Tên loại thiết bị',
      field: 'name',
      render: (item) => <span className="fw-semibold text-dark">{item.name}</span>,
    },
    {
      header: 'Mô tả',
      field: 'description',
      render: (item) => item.description || <span className="text-muted fst-italic">Chưa có mô tả</span>,
    },
    {
      header: 'Thao tác',
      align: 'end',
      render: (item) => (
        <Button
          variant="outline-primary"
          size="sm"
          onClick={() => handleOpenTypeModal(item)}
          title="Chỉnh sửa loại thiết bị"
        >
          <i className="bi bi-pencil"></i> Sửa
        </Button>
      ),
    },
  ];

  // Location columns
  const locColumns = [
    {
      header: 'ID',
      field: 'locationId',
      width: '80px',
      render: (item) => item.locationId || item.id,
    },
    {
      header: 'Phòng (Room)',
      field: 'room',
      render: (item) => <span className="fw-semibold text-dark">{item.room}</span>,
    },
    {
      header: 'Khu vực (Area)',
      field: 'area',
      render: (item) => item.area || '—',
    },
    {
      header: 'Khoa / Phòng ban',
      field: 'department',
      render: (item) => item.department || <span className="text-muted fst-italic">Mặc định</span>,
    },
    {
      header: 'Thao tác',
      align: 'end',
      render: (item) => (
        <Button
          variant="outline-primary"
          size="sm"
          onClick={() => handleOpenLocModal(item)}
          title="Chỉnh sửa vị trí"
        >
          <i className="bi bi-pencil"></i> Sửa
        </Button>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Danh mục Loại Thiết bị & Vị trí"
        subtitle="Quản lý các chủng loại máy móc và danh sách phòng lab, khu vực lắp đặt"
        breadcrumbs={[
          { label: 'Bàn làm việc', to: `${basePath}/dashboard` },
          { label: 'Loại thiết bị & Vị trí' },
        ]}
      />

      <Card className="border border-light-subtle shadow-sm">
        <Card.Body className="p-3">
          <Tabs activeKey={activeTab} onSelect={handleTabSelect} className="mb-4">
            <Tab
              eventKey="types"
              title={
                <span className="d-inline-flex align-items-center gap-2">
                  <i className="bi bi-tags"></i>
                  <span>Loại thiết bị ({types.length})</span>
                </span>
              }
            >
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h6 className="fw-bold text-dark mb-0">Danh sách chủng loại thiết bị</h6>
                <Button
                  variant="primary"
                  size="sm"
                  className="d-inline-flex align-items-center gap-1"
                  onClick={() => handleOpenTypeModal(null)}
                >
                  <i className="bi bi-plus-lg"></i>
                  <span>Thêm loại thiết bị</span>
                </Button>
              </div>

              <DataTable
                columns={typeColumns}
                data={types}
                loading={typesLoading}
                error={typesError}
                onRetry={fetchTypes}
                emptyTitle="Chưa có loại thiết bị"
                emptyMessage="Nhấn 'Thêm loại thiết bị' để khởi tạo danh mục đầu tiên."
                keyField="typeId"
              />
            </Tab>

            <Tab
              eventKey="locations"
              title={
                <span className="d-inline-flex align-items-center gap-2">
                  <i className="bi bi-geo-alt"></i>
                  <span>Vị trí lắp đặt ({locations.length})</span>
                </span>
              }
            >
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h6 className="fw-bold text-dark mb-0">Danh sách phòng lab & vị trí lắp đặt</h6>
                <Button
                  variant="primary"
                  size="sm"
                  className="d-inline-flex align-items-center gap-1"
                  onClick={() => handleOpenLocModal(null)}
                >
                  <i className="bi bi-plus-lg"></i>
                  <span>Thêm vị trí mới</span>
                </Button>
              </div>

              <DataTable
                columns={locColumns}
                data={locations}
                loading={locsLoading}
                error={locsError}
                onRetry={fetchLocations}
                emptyTitle="Chưa có vị trí lắp đặt"
                emptyMessage="Nhấn 'Thêm vị trí mới' để khai báo phòng máy đầu tiên."
                keyField="locationId"
              />
            </Tab>
          </Tabs>
        </Card.Body>
      </Card>

      {/* Modal Add/Edit Type */}
      <Modal show={showTypeModal} onHide={() => !typeSubmitting && setShowTypeModal(false)} centered>
        <Form onSubmit={handleSaveType}>
          <Modal.Header closeButton={!typeSubmitting} className="border-bottom border-light-subtle">
            <Modal.Title className="fs-6 fw-bold text-dark">
              {editingType ? 'Chỉnh sửa loại thiết bị' : 'Thêm loại thiết bị mới'}
            </Modal.Title>
          </Modal.Header>
          <Modal.Body className="p-4">
            {typeModalError && (
              <Alert variant="danger" className="py-2 small">
                {typeModalError}
              </Alert>
            )}
            <Form.Group className="mb-3">
              <Form.Label className="small fw-semibold">
                Tên loại thiết bị <span className="text-danger">*</span>
              </Form.Label>
              <Form.Control
                type="text"
                placeholder="Ví dụ: Máy ly tâm, Kính hiển vi quang học"
                value={typeName}
                onChange={(e) => setTypeName(e.target.value)}
                required
                autoFocus
              />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label className="small fw-semibold">Mô tả chi tiết</Form.Label>
              <Form.Control
                as="textarea"
                rows={3}
                placeholder="Mô tả chức năng hoặc mục đích sử dụng..."
                value={typeDesc}
                onChange={(e) => setTypeDesc(e.target.value)}
              />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer className="border-top border-light-subtle">
            <Button
              variant="outline-secondary"
              size="sm"
              onClick={() => setShowTypeModal(false)}
              disabled={typeSubmitting}
            >
              Hủy
            </Button>
            <Button
              variant="primary"
              size="sm"
              type="submit"
              disabled={typeSubmitting}
              className="d-inline-flex align-items-center gap-1"
            >
              {typeSubmitting && <Spinner size="sm" animation="border" />}
              <span>{editingType ? 'Cập nhật' : 'Tạo mới'}</span>
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* Modal Add/Edit Location */}
      <Modal show={showLocModal} onHide={() => !locSubmitting && setShowLocModal(false)} centered>
        <Form onSubmit={handleSaveLocation}>
          <Modal.Header closeButton={!locSubmitting} className="border-bottom border-light-subtle">
            <Modal.Title className="fs-6 fw-bold text-dark">
              {editingLoc ? 'Chỉnh sửa vị trí lắp đặt' : 'Thêm vị trí mới'}
            </Modal.Title>
          </Modal.Header>
          <Modal.Body className="p-4">
            {locModalError && (
              <Alert variant="danger" className="py-2 small">
                {locModalError}
              </Alert>
            )}
            <Form.Group className="mb-3">
              <Form.Label className="small fw-semibold">
                Phòng (Room) <span className="text-danger">*</span>
              </Form.Label>
              <Form.Control
                type="text"
                placeholder="Ví dụ: P.101, Lab-204"
                value={room}
                onChange={(e) => setRoom(e.target.value)}
                required
                autoFocus
              />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label className="small fw-semibold">
                Khu vực (Area) <span className="text-danger">*</span>
              </Form.Label>
              <Form.Control
                type="text"
                placeholder="Ví dụ: Khu A, Tòa Alpha, Tầng 2"
                value={area}
                onChange={(e) => setArea(e.target.value)}
                required
              />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label className="small fw-semibold">Khoa / Phòng ban</Form.Label>
              <Form.Control
                type="text"
                placeholder="Ví dụ: Phòng Thí Nghiệm Hóa Sinh, Bộ Môn Điện Tử"
                value={department}
                onChange={(e) => setDepartment(e.target.value)}
              />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer className="border-top border-light-subtle">
            <Button
              variant="outline-secondary"
              size="sm"
              onClick={() => setShowLocModal(false)}
              disabled={locSubmitting}
            >
              Hủy
            </Button>
            <Button
              variant="primary"
              size="sm"
              type="submit"
              disabled={locSubmitting}
              className="d-inline-flex align-items-center gap-1"
            >
              {locSubmitting && <Spinner size="sm" animation="border" />}
              <span>{editingLoc ? 'Cập nhật' : 'Tạo mới'}</span>
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </div>
  );
}

