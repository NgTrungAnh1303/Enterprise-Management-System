import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import Form from 'react-bootstrap/Form';
import Button from 'react-bootstrap/Button';
import { useAuth } from '../../auth/AuthContext';
import { equipmentApi, equipmentTypesApi, locationsApi } from '../../api';
import PageHeader from '../../components/common/PageHeader';
import FilterBar from '../../components/common/FilterBar';
import SearchBar from '../../components/common/SearchBar';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';

export default function EquipmentListPage() {
  const navigate = useNavigate();
  const { role } = useAuth();

  // State
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filters & Pagination
  const [query, setQuery] = useState('');
  const [typeId, setTypeId] = useState('');
  const [locationId, setLocationId] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [total, setTotal] = useState(0);

  // Metadata for filter dropdowns
  const [types, setTypes] = useState([]);
  const [locations, setLocations] = useState([]);

  // Determine base path for role
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

  // Load filter options
  useEffect(() => {
    let isMounted = true;
    const loadMetadata = async () => {
      try {
        const [typesRes, locsRes] = await Promise.all([
          equipmentTypesApi.getEquipmentTypes(),
          locationsApi.getLocations(),
        ]);
        if (isMounted) {
          setTypes(Array.isArray(typesRes) ? typesRes : (typesRes?.items || typesRes?.data || []));
          setLocations(Array.isArray(locsRes) ? locsRes : (locsRes?.items || locsRes?.data || []));
        }
      } catch (err) {
        console.warn('Không thể nạp danh mục phân loại:', err);
      }
    };
    loadMetadata();
    return () => {
      isMounted = false;
    };
  }, []);

  // Fetch equipment list from server
  const fetchEquipment = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = {
        page,
        size,
      };
      if (query.trim()) params.q = query.trim();
      if (typeId) params.typeId = typeId;
      if (locationId) params.locationId = locationId;
      if (status) params.status = status;

      const res = await equipmentApi.getEquipment(params);
      const resData = res?.data || res || {};
      setData(resData.items || []);
      setTotal(resData.total || 0);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, [page, size, query, typeId, locationId, status]);

  useEffect(() => {
    fetchEquipment();
  }, [fetchEquipment]);

  const handleResetFilters = () => {
    setQuery('');
    setTypeId('');
    setLocationId('');
    setStatus('');
    setPage(0);
  };

  const handleSearch = (val) => {
    setQuery(val);
    setPage(0);
  };

  const basePath = getBasePath();
  const canCreate = role === 'ADMIN' || role === 'MANAGER';
  const canEdit = role === 'ADMIN' || role === 'MANAGER';
  const canViewHistory = role === 'MANAGER' || role === 'TECHNICIAN';

  // Define columns
  const columns = [
    {
      header: 'Mã thiết bị',
      field: 'code',
      render: (item) => (
        <span
          className="fw-bold font-monospace text-primary text-decoration-underline"
          role="button"
          tabIndex={0}
          onClick={() => navigate(`${basePath}/equipment/${item.equipmentId || item.id}`)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') navigate(`${basePath}/equipment/${item.equipmentId || item.id}`);
          }}
          title="Xem chi tiết thiết bị"
        >
          {item.code}
        </span>
      ),
    },
    {
      header: 'Loại thiết bị',
      field: 'type',
      render: (item) => item.type?.name || '—',
    },
    {
      header: 'Hãng & Model',
      render: (item) => (
        <div>
          <div className="fw-semibold text-dark">{item.brand || '—'}</div>
          <small className="text-secondary">{item.model || ''}</small>
        </div>
      ),
    },
    {
      header: 'Vị trí lắp đặt',
      render: (item) =>
        item.location ? (
          <div>
            <span className="fw-medium">{item.location.room}</span>
            {item.location.area && (
              <span className="text-secondary small ms-1">({item.location.area})</span>
            )}
          </div>
        ) : (
          '—'
        ),
    },
    {
      header: 'Trạng thái vận hành',
      field: 'status',
      align: 'center',
      render: (item) => <StatusBadge type="equipment" value={item.status} />,
    },
    {
      header: 'Kế hoạch bảo trì',
      align: 'center',
      render: (item) => (
        <div className="d-flex flex-column align-items-center gap-1">
          <StatusBadge type="due" value={item.dueLabel || 'NORMAL'} />
          {item.nextDueDate && (
            <small className="text-secondary font-monospace" style={{ fontSize: '0.75rem' }}>
              Hạn: {item.nextDueDate}
            </small>
          )}
        </div>
      ),
    },
    {
      header: 'Thao tác',
      align: 'end',
      render: (item) => {
        const eqId = item.equipmentId || item.id;
        return (
          <div className="d-flex align-items-center justify-content-end gap-1">
            <Button
              variant="outline-secondary"
              size="sm"
              onClick={() => navigate(`${basePath}/equipment/${eqId}`)}
              title="Xem chi tiết"
            >
              <i className="bi bi-eye"></i>
            </Button>

            {canEdit && (
              <Button
                variant="outline-primary"
                size="sm"
                onClick={() => navigate(`${basePath}/equipment/${eqId}/edit`)}
                title="Chỉnh sửa thông tin"
              >
                <i className="bi bi-pencil"></i>
              </Button>
            )}

            {canViewHistory && (
              <Button
                variant="outline-info"
                size="sm"
                onClick={() => navigate(`${basePath}/equipment/${eqId}/history`)}
                title="Lịch sử bảo trì"
              >
                <i className="bi bi-clock-history"></i>
              </Button>
            )}
          </div>
        );
      },
    },
  ];

  return (
    <div>
      <PageHeader
        title="Danh mục & Tìm kiếm Thiết bị"
        subtitle="Quản lý và tra cứu trạng thái vận hành, kỳ hạn bảo dưỡng của toàn bộ thiết bị trong đơn vị"
        breadcrumbs={[
          { label: 'Bàn làm việc', to: `${basePath}/dashboard` },
          { label: 'Danh mục thiết bị' },
        ]}
        actions={
          canCreate ? (
            <Button
              variant="primary"
              className="d-inline-flex align-items-center gap-2"
              onClick={() => navigate(`${basePath}/equipment/new`)}
            >
              <i className="bi bi-plus-lg"></i>
              <span>Thêm thiết bị mới</span>
            </Button>
          ) : null
        }
      />

      <FilterBar onReset={handleResetFilters}>
        <div style={{ minWidth: '220px', flexGrow: 1 }}>
          <SearchBar
            value={query}
            onSearch={handleSearch}
            placeholder="Tìm theo mã, hãng, model..."
          />
        </div>

        <Form.Select
          size="sm"
          value={typeId}
          onChange={(e) => {
            setTypeId(e.target.value);
            setPage(0);
          }}
          style={{ width: 'auto', minWidth: '160px' }}
          aria-label="Lọc theo loại thiết bị"
        >
          <option value="">Tất cả loại thiết bị</option>
          {types.map((t) => (
            <option key={t.typeId || t.id} value={t.typeId || t.id}>
              {t.name}
            </option>
          ))}
        </Form.Select>

        <Form.Select
          size="sm"
          value={locationId}
          onChange={(e) => {
            setLocationId(e.target.value);
            setPage(0);
          }}
          style={{ width: 'auto', minWidth: '150px' }}
          aria-label="Lọc theo vị trí"
        >
          <option value="">Tất cả vị trí</option>
          {locations.map((loc) => (
            <option key={loc.locationId || loc.id} value={loc.locationId || loc.id}>
              {loc.room} {loc.area ? `(${loc.area})` : ''}
            </option>
          ))}
        </Form.Select>

        <Form.Select
          size="sm"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value);
            setPage(0);
          }}
          style={{ width: 'auto', minWidth: '150px' }}
          aria-label="Lọc theo trạng thái"
        >
          <option value="">Tất cả trạng thái</option>
          <option value="ACTIVE">Hoạt động (ACTIVE)</option>
          <option value="UNDER_MAINTENANCE">Đang bảo dưỡng</option>
          <option value="OUT_OF_SERVICE">Ngừng hoạt động</option>
        </Form.Select>
      </FilterBar>

      <DataTable
        columns={columns}
        data={data}
        loading={loading}
        error={error}
        onRetry={fetchEquipment}
        emptyTitle="Không tìm thấy thiết bị"
        emptyMessage="Không có thiết bị nào phù hợp với bộ lọc tìm kiếm hiện tại."
        pagination={{
          page,
          size,
          total,
          onPageChange: (p) => setPage(p),
          onSizeChange: (s) => {
            setSize(s);
            setPage(0);
          },
        }}
        keyField="equipmentId"
      />
    </div>
  );
}

