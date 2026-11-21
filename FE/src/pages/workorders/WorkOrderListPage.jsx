import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import Form from 'react-bootstrap/Form';
import Button from 'react-bootstrap/Button';
import Badge from 'react-bootstrap/Badge';
import { useAuth } from '../../auth/AuthContext';
import { workOrdersApi, equipmentApi } from '../../api';
import PageHeader from '../../components/common/PageHeader';
import FilterBar from '../../components/common/FilterBar';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';

export default function WorkOrderListPage() {
  const navigate = useNavigate();
  const { role } = useAuth();

  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

   const [status, setStatus] = useState('');
  const [equipmentId, setEquipmentId] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [total, setTotal] = useState(0);

  const [equipmentList, setEquipmentList] = useState([]);

  const isManager = role === 'MANAGER';
  const isTechnician = role === 'TECHNICIAN';
  const basePath = isManager ? '/manager' : '/technician';

   useEffect(() => {
    let isMounted = true;
    const loadEq = async () => {
      try {
        const res = await equipmentApi.getEquipment({ size: 100 });
        if (isMounted) {
          setEquipmentList(res?.items || res?.data?.items || []);
        }
      } catch (err) {
        console.warn('Không thể nạp danh sách thiết bị cho bộ lọc WO:', err);
      }
    };
    loadEq();
    return () => {
      isMounted = false;
    };
  }, []);

   const fetchWorkOrders = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = {
        page,
        size,
      };
      if (status) params.status = status;
      if (equipmentId) params.equipmentId = equipmentId;

      const res = await workOrdersApi.getWorkOrders(params);
      const resData = res?.data || res || {};
      setData(resData.items || []);
      setTotal(resData.total || 0);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, [page, size, status, equipmentId]);

  useEffect(() => {
    fetchWorkOrders();
  }, [fetchWorkOrders]);

  const handleResetFilters = () => {
    setStatus('');
    setEquipmentId('');
    setPage(0);
  };

  const columns = [
    {
      header: 'Mã Work Order',
      field: 'woCode',
      render: (item) => (
        <span
          className="fw-bold font-monospace text-primary text-decoration-underline"
          role="button"
          tabIndex={0}
          onClick={() => navigate(`${basePath}/work-orders/${item.woId || item.id}`)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') navigate(`${basePath}/work-orders/${item.woId || item.id}`);
          }}
          title="Xem chi tiết phiếu bảo trì"
        >
          {item.woCode}
        </span>
      ),
    },
    {
      header: 'Thiết bị',
      render: (item) => (
        <div>
          <span className="fw-semibold text-dark">{item.equipmentCode || '—'}</span>
          {item.equipmentBrand && (
            <small className="text-secondary ms-1">
              ({item.equipmentBrand} {item.equipmentModel || ''})
            </small>
          )}
        </div>
      ),
    },
    {
      header: 'Phân loại',
      render: (item) => (
        <Badge
          bg={item.type === 'PERIODIC' ? 'info-subtle' : 'warning-subtle'}
          className={`border ${
            item.type === 'PERIODIC'
              ? 'text-info-emphasis border-info-subtle'
              : 'text-warning-emphasis border-warning-subtle'
          }`}
        >
          {item.type === 'PERIODIC' ? 'Định kỳ' : 'Sửa chữa'}
        </Badge>
      ),
    },
    {
      header: 'Kỳ bảo trì',
      field: 'period',
      render: (item) => (
        <span className="font-monospace text-secondary small">
          {item.period || '—'}
        </span>
      ),
    },
    {
      header: 'Kỹ thuật viên phụ trách',
      render: (item) => item.technicianName || <span className="text-muted fst-italic">Chưa phân công</span>,
    },
    {
      header: 'Trạng thái',
      field: 'status',
      align: 'center',
      render: (item) => <StatusBadge type="workOrder" value={item.status} />,
    },
    {
      header: 'Thời điểm tạo',
      render: (item) => (
        <span className="font-monospace text-secondary small">
          {item.createdAt ? new Date(item.createdAt).toLocaleDateString('vi-VN') : '—'}
        </span>
      ),
    },
    {
      header: 'Thao tác',
      align: 'end',
      render: (item) => {
        const woId = item.woId || item.id;
        return (
          <div className="d-flex justify-content-end gap-1">
            {isTechnician && (
              <Button
                variant="primary"
                size="sm"
                onClick={() => navigate(`/technician/work-orders/${woId}`)}
                className="d-inline-flex align-items-center gap-1"
              >
                <i className="bi bi-play-circle"></i>
                <span>Thực hiện</span>
              </Button>
            )}

            {isManager && (
              <Button
                variant={item.status === 'SUBMITTED' ? 'success' : 'outline-primary'}
                size="sm"
                onClick={() => navigate(`/manager/work-orders/${woId}`)}
                className="d-inline-flex align-items-center gap-1"
              >
                <i className={item.status === 'SUBMITTED' ? 'bi bi-check-circle' : 'bi bi-eye'}></i>
                <span>{item.status === 'SUBMITTED' ? 'Nghiệm thu' : 'Chi tiết'}</span>
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
        title={
          isTechnician
            ? 'Phiếu Bảo trì Được Phân công'
            : 'Quản lý Phiếu Bảo trì (Work Orders)'
        }
        subtitle={
          isTechnician
            ? 'Danh sách các Work Order được giao cho bạn thực hiện kiểm tra và sửa chữa'
            : 'Theo dõi toàn bộ phiếu bảo trì định kỳ và sửa chữa sự cố trong toàn hệ thống'
        }
        breadcrumbs={[
          { label: 'Bàn làm việc', to: `${basePath}/dashboard` },
          { label: 'Phiếu bảo trì' },
        ]}
        actions={
          isManager ? (
            <Button
              variant="primary"
              className="d-inline-flex align-items-center gap-2"
              onClick={() => navigate('/manager/work-orders/new')}
            >
              <i className="bi bi-plus-lg"></i>
              <span>Tạo Work Order mới</span>
            </Button>
          ) : null
        }
      />

      <FilterBar onReset={handleResetFilters}>
        <Form.Select
          size="sm"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value);
            setPage(0);
          }}
          style={{ width: 'auto', minWidth: '180px' }}
          aria-label="Lọc theo trạng thái WO"
        >
          <option value="">Tất cả trạng thái</option>
          <option value="OPEN">Mới tạo (OPEN)</option>
          <option value="ASSIGNED">Đã phân công (ASSIGNED)</option>
          <option value="IN_PROGRESS">Đang thực hiện (IN_PROGRESS)</option>
          <option value="SUBMITTED">Chờ nghiệm thu (SUBMITTED)</option>
          <option value="CLOSED">Đã đóng (CLOSED)</option>
        </Form.Select>

        <Form.Select
          size="sm"
          value={equipmentId}
          onChange={(e) => {
            setEquipmentId(e.target.value);
            setPage(0);
          }}
          style={{ width: 'auto', minWidth: '220px' }}
          aria-label="Lọc theo thiết bị"
        >
          <option value="">Tất cả thiết bị</option>
          {equipmentList.map((eq) => (
            <option key={eq.equipmentId || eq.id} value={eq.equipmentId || eq.id}>
              {eq.code} - {eq.brand || ''} {eq.model || ''}
            </option>
          ))}
        </Form.Select>
      </FilterBar>

      <DataTable
        columns={columns}
        data={data}
        loading={loading}
        error={error}
        onRetry={fetchWorkOrders}
        emptyTitle="Không có Work Order nào"
        emptyMessage={
          isTechnician
            ? 'Bạn hiện không có phiếu bảo trì nào được phân công phù hợp với bộ lọc.'
            : 'Chưa có phiếu bảo trì nào phù hợp với bộ lọc tìm kiếm.'
        }
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
        keyField="woId"
      />
    </div>
  );
}

