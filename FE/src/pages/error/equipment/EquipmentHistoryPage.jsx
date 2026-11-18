import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Card from 'react-bootstrap/Card';
import Button from 'react-bootstrap/Button';
import Badge from 'react-bootstrap/Badge';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Alert from 'react-bootstrap/Alert';
import { equipmentApi } from '../../api';
import PageHeader from '../../components/common/PageHeader';
import DataTable from '../../components/common/DataTable';
import LoadingState from '../../components/common/LoadingState';
import ErrorState from '../../components/common/ErrorState';

export default function EquipmentHistoryPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [equipment, setEquipment] = useState(null);
  const [data, setData] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchHistory = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [eqRes, histRes] = await Promise.all([
        equipmentApi.getEquipmentById(id),
        equipmentApi.getEquipmentHistory(id, { page, size }),
      ]);

      const eqData = eqRes?.data || eqRes;
      setEquipment(eqData);

      const histData = histRes?.items ? histRes : (histRes?.data || {});
      setData(histData.items || []);
      setTotal(histData.total || 0);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, [id, page, size]);

  useEffect(() => {
    fetchHistory();
  }, [fetchHistory]);

  const columns = [
    {
      header: 'Thời điểm',
      field: 'performedAt',
      render: (row) => (
        <span className="font-monospace text-secondary small">
          {row.performedAt ? new Date(row.performedAt).toLocaleString('vi-VN') : '—'}
        </span>
      ),
    },
    {
      header: 'Thao tác nghiệp vụ',
      field: 'action',
      render: (row) => (
        <span className="badge bg-light text-dark border font-monospace">
          {row.action || '—'}
        </span>
      ),
    },
    {
      header: 'Work Order liên kết',
      field: 'woCode',
      render: (row) =>
        row.woCode ? (
          <span className="font-monospace fw-bold text-primary">{row.woCode}</span>
        ) : (
          <span className="text-muted small fst-italic">Không có</span>
        ),
    },
    {
      header: 'Trạng thái chuyển dịch',
      field: 'statusChange',
      render: (row) => (
        <div className="d-flex align-items-center gap-1 small">
          <Badge bg="secondary">{row.oldStatus || '—'}</Badge>
          <i className="bi bi-arrow-right text-muted"></i>
          <Badge bg="primary">{row.newStatus || '—'}</Badge>
        </div>
      ),
    },
    {
      header: 'Người thực hiện',
      field: 'performedByName',
      render: (row) => (
        <span className="fw-semibold text-dark small">{row.performedByName || 'Hệ thống'}</span>
      ),
    },
    {
      header: 'Ghi chú / Lý do',
      field: 'note',
      render: (row) => (
        <div className="text-truncate small text-secondary" style={{ maxWidth: '240px' }} title={row.note}>
          {row.note || '—'}
        </div>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title={`Lịch sử Bảo trì: ${equipment?.code || `EQ-${id}`}`}
        subtitle={`Nhật ký các lần bảo dưỡng, nghiệm thu và điều chỉnh kỹ thuật (Bất biến / Append-only)`}
        actions={
          <Button variant="outline-secondary" size="sm" onClick={() => navigate(-1)}>
            <i className="bi bi-arrow-left me-1"></i>Quay lại chi tiết thiết bị
          </Button>
        }
      />

      {/* Equipment Summary Banner */}
      {equipment && (
        <Card className="border border-light-subtle shadow-sm mb-4">
          <Card.Body className="p-3">
            <Row className="g-3 align-items-center">
              <Col md={3}>
                <div className="text-secondary small">Mã thiết bị</div>
                <div className="fw-bold text-primary font-monospace">{equipment.code}</div>
              </Col>
              <Col md={3}>
                <div className="text-secondary small">Thương hiệu / Model</div>
                <div className="fw-semibold text-dark">{equipment.brand} {equipment.model}</div>
              </Col>
              <Col md={3}>
                <div className="text-secondary small">Vị trí lắp đặt</div>
                <div>{equipment.location?.name || '—'}</div>
              </Col>
              <Col md={3}>
                <div className="text-secondary small">Trạng thái hiện tại</div>
                <Badge bg={equipment.status === 'ACTIVE' ? 'success' : equipment.status === 'UNDER_MAINTENANCE' ? 'warning' : 'danger'}>
                  {equipment.status}
                </Badge>
              </Col>
            </Row>
          </Card.Body>
        </Card>
      )}

      {/* Invariant Banner */}
      <Alert variant="info" className="d-flex align-items-center gap-2 py-2 mb-4 border-info small">
        <i className="bi bi-shield-lock-fill fs-5 text-info"></i>
        <span>
          <strong>Lưu ý an ninh:</strong> Bảng lịch sử bảo trì tuân thủ nghiêm ngặt nguyên tắc Append-only. Toàn bộ bản ghi không thể bị sửa đổi hoặc xóa bỏ dưới bất kỳ hình thức nào.
        </span>
      </Alert>

      {/* History Table */}
      {loading ? (
        <LoadingState message="Đang nạp nhật ký lịch sử bảo dưỡng..." />
      ) : error ? (
        <ErrorState
          title={error.status === 403 ? 'Truy cập bị từ chối (403 Forbidden)' : 'Không thể tải lịch sử'}
          message={
            error.status === 403
              ? 'Bạn không có quyền truy cập lịch sử bảo trì của thiết bị này (chống BOLA/IDOR).'
              : error.message
          }
          code={error.code}
          onRetry={fetchHistory}
        />
      ) : (
        <DataTable
          columns={columns}
          data={data}
          keyField="historyId"
          emptyTitle="Chưa có lịch sử bảo trì"
          emptyMessage="Thiết bị này chưa có bất kỳ thao tác bảo trì, sửa chữa hoặc đổi trạng thái nào được ghi nhận."
          pagination={{
            page,
            size,
            total,
            onPageChange: (newPage) => setPage(newPage),
            onSizeChange: (newSize) => {
              setSize(newSize);
              setPage(0);
            },
          }}
        />
      )}
    </div>
  );
}
