import React, { useState, useEffect, useCallback } from 'react';
import Card from 'react-bootstrap/Card';
import Button from 'react-bootstrap/Button';
import Badge from 'react-bootstrap/Badge';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Modal from 'react-bootstrap/Modal';
import { auditApi } from '../../api';
import PageHeader from '../../components/common/PageHeader';
import DataTable from '../../components/common/DataTable';
import LoadingState from '../../components/common/LoadingState';
import ErrorState from '../../components/common/ErrorState';

export default function AuditLogPage() {
  const [data, setData] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(15);
  const [actionFilter, setActionFilter] = useState('');
  const [outcomeFilter, setOutcomeFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Detail Modal
  const [selectedLog, setSelectedLog] = useState(null);

  const fetchAuditLogs = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = { page, size };
      if (actionFilter.trim()) params.action = actionFilter.trim();
      if (outcomeFilter) params.outcome = outcomeFilter;

      const res = await auditApi.getAuditLogs(params);
      const resData = res?.items ? res : (res?.data || {});
      setData(resData.items || []);
      setTotal(resData.total || 0);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, [page, size, actionFilter, outcomeFilter]);

  useEffect(() => {
    fetchAuditLogs();
  }, [fetchAuditLogs]);

  const isSecurityViolation = (row) => {
    if (row.outcome === 'DENIED' || row.outcome === 'FAILED' || row.outcome === 'ALERT') {
      return true;
    }
    const code = row.reasonCode || '';
    return (
      code.includes('SELF_ACCEPT') ||
      code.includes('NOT_MANAGER') ||
      code.includes('WO_NOT_ASSIGNED') ||
      code.includes('CANNOT_LOCK_SELF') ||
      code.includes('CANNOT_DEMOTE_SELF') ||
      code.includes('ACCESS_DENIED')
    );
  };

  const columns = [
    {
      header: 'Thời điểm',
      field: 'occurredAt',
      render: (row) => (
        <span className="font-monospace text-secondary small">
          {row.occurredAt ? new Date(row.occurredAt).toLocaleString('vi-VN') : '—'}
        </span>
      ),
    },
    {
      header: 'Người thực hiện',
      field: 'actor',
      render: (row) => (
        <div>
          <Badge bg={row.actorRole === 'ADMIN' ? 'danger' : row.actorRole === 'MANAGER' ? 'warning' : 'info'}>
            {row.actorRole || 'UNKNOWN'}
          </Badge>
          {row.actorId && (
            <span className="font-monospace small text-muted ms-1">#{row.actorId}</span>
          )}
        </div>
      ),
    },
    {
      header: 'Thao tác (Action)',
      field: 'action',
      render: (row) => (
        <span className="badge bg-light text-dark border font-monospace">
          {row.action || '—'}
        </span>
      ),
    },
    {
      header: 'Đối tượng đích',
      field: 'target',
      render: (row) => (
        <div className="small">
          <span className="text-secondary">{row.targetType}: </span>
          <span className="font-monospace fw-bold text-dark">{row.targetId || '—'}</span>
        </div>
      ),
    },
    {
      header: 'Kết quả (Outcome)',
      field: 'outcome',
      align: 'center',
      render: (row) => {
        const isViolation = isSecurityViolation(row);
        const colorMap = {
          SUCCESS: 'success',
          DENIED: 'danger',
          FAILED: 'danger',
          ALERT: 'warning',
        };
        return (
          <Badge
            bg={colorMap[row.outcome] || 'secondary'}
            className={isViolation ? 'px-2 py-1 border border-danger shadow-sm' : ''}
          >
            {row.outcome}
            {isViolation && ' ⚠️'}
          </Badge>
        );
      },
    },
    {
      header: 'Mã lý do (Reason Code)',
      field: 'reasonCode',
      render: (row) => {
        const isViolation = isSecurityViolation(row);
        return (
          <span
            className={`font-monospace small ${
              isViolation ? 'text-danger fw-bold bg-danger-subtle px-2 py-1 rounded border border-danger' : 'text-secondary'
            }`}
          >
            {row.reasonCode || 'OK'}
          </span>
        );
      },
    },
    {
      header: 'Thao tác',
      align: 'end',
      render: (row) => (
        <Button
          variant="outline-secondary"
          size="sm"
          className="py-0 px-2"
          onClick={() => setSelectedLog(row)}
        >
          Chi tiết
        </Button>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Nhật ký Hệ thống & Kiểm toán An toàn (Audit Log)"
        subtitle="Giám sát an ninh, phát hiện các nỗ lực vi phạm phân quyền và truy vết mọi giao dịch (Append-only / SCR-18)"
      />

      {/* Filter Row */}
      <Card className="border border-light-subtle shadow-sm mb-4">
        <Card.Body className="p-3">
          <Row className="g-3 align-items-end">
            <Col xs={12} sm={6} md={4}>
              <Form.Label className="small fw-semibold text-secondary">Lọc theo hành động (Action)</Form.Label>
              <Form.Control
                type="text"
                size="sm"
                placeholder="Ví dụ: LOGIN, WO_ACCEPT, ACCESS_DENIED..."
                value={actionFilter}
                onChange={(e) => {
                  setActionFilter(e.target.value);
                  setPage(0);
                }}
              />
            </Col>

            <Col xs={12} sm={6} md={3}>
              <Form.Label className="small fw-semibold text-secondary">Kết quả kiểm toán (Outcome)</Form.Label>
              <Form.Select
                size="sm"
                value={outcomeFilter}
                onChange={(e) => {
                  setOutcomeFilter(e.target.value);
                  setPage(0);
                }}
              >
                <option value="">Tất cả kết quả</option>
                <option value="SUCCESS">Thành công (SUCCESS)</option>
                <option value="DENIED">Bị từ chối an ninh (DENIED)</option>
                <option value="FAILED">Thất bại nghiệp vụ (FAILED)</option>
                <option value="ALERT">Cảnh báo rủi ro (ALERT)</option>
              </Form.Select>
            </Col>

            <Col xs={12} sm={6} md={3}>
              <Button
                variant="outline-secondary"
                size="sm"
                onClick={() => {
                  setActionFilter('');
                  setOutcomeFilter('');
                  setPage(0);
                }}
              >
                Đặt lại bộ lọc
              </Button>
            </Col>
          </Row>
        </Card.Body>
      </Card>

      {/* Security Invariant Legend */}
      <div className="d-flex align-items-center gap-3 mb-3 p-2 bg-light rounded border small">
        <span className="fw-semibold text-dark">Quy tắc nhận diện rủi ro:</span>
        <span className="badge bg-danger-subtle text-danger border border-danger">
          SELF_ACCEPT / NOT_MANAGER / WO_NOT_ASSIGNED / DENIED
        </span>
        <span className="text-muted">
          Các bản ghi có vi phạm phân quyền hoặc nỗ lực vượt quyền BOLA/IDOR được tự động đóng khung đỏ nổi bật.
        </span>
      </div>

      {/* Table */}
      {loading ? (
        <LoadingState message="Đang nạp dữ liệu kiểm toán hệ thống..." />
      ) : error ? (
        <ErrorState
          title="Không thể tải Audit Log"
          message={error.message}
          code={error.code}
          onRetry={fetchAuditLogs}
        />
      ) : (
        <DataTable
          columns={columns}
          data={data}
          keyField="logId"
          emptyTitle="Chưa có bản ghi kiểm toán nào"
          emptyMessage="Không tìm thấy nhật ký kiểm toán phù hợp."
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

      {/* Modal: View Details */}
      <Modal show={Boolean(selectedLog)} onHide={() => setSelectedLog(null)} size="lg" centered>
        <Modal.Header closeButton className="bg-light border-bottom">
          <Modal.Title className="h6 fw-bold font-monospace">
            Bản ghi kiểm toán #{selectedLog?.logId} — {selectedLog?.action}
          </Modal.Title>
        </Modal.Header>
        <Modal.Body className="p-4">
          {selectedLog && (
            <div>
              <Row className="g-3 mb-3">
                <Col md={6}>
                  <div className="text-secondary small">Thời điểm diễn ra</div>
                  <div className="font-monospace small">
                    {new Date(selectedLog.occurredAt).toLocaleString('vi-VN')}
                  </div>
                </Col>
                <Col md={6}>
                  <div className="text-secondary small">Người thao tác</div>
                  <div className="fw-semibold">
                    {selectedLog.actorRole} (#{selectedLog.actorId || 'N/A'})
                  </div>
                </Col>
                <Col md={6}>
                  <div className="text-secondary small">Hành động & Kết quả</div>
                  <div>
                    <span className="font-monospace me-2">{selectedLog.action}</span>
                    <Badge bg={selectedLog.outcome === 'SUCCESS' ? 'success' : 'danger'}>
                      {selectedLog.outcome}
                    </Badge>
                  </div>
                </Col>
                <Col md={6}>
                  <div className="text-secondary small">Mã lý do (Reason Code)</div>
                  <div className="font-monospace fw-bold text-danger">
                    {selectedLog.reasonCode || 'OK'}
                  </div>
                </Col>
                <Col md={6}>
                  <div className="text-secondary small">Request ID (Truy vết)</div>
                  <div className="font-monospace small text-muted">
                    {selectedLog.requestId || '—'}
                  </div>
                </Col>
                <Col md={6}>
                  <div className="text-secondary small">IP nguồn (Source IP)</div>
                  <div className="font-monospace small text-muted">
                    {selectedLog.sourceIp || '—'}
                  </div>
                </Col>
              </Row>

              {selectedLog.oldValue && (
                <div className="mb-3">
                  <div className="text-secondary small fw-semibold mb-1">Giá trị cũ (Old Value):</div>
                  <pre className="p-2 bg-light border rounded small font-monospace">
                    {selectedLog.oldValue}
                  </pre>
                </div>
              )}

              {selectedLog.newValue && (
                <div className="mb-0">
                  <div className="text-secondary small fw-semibold mb-1">Giá trị mới (New Value):</div>
                  <pre className="p-2 bg-light border rounded small font-monospace">
                    {selectedLog.newValue}
                  </pre>
                </div>
              )}
            </div>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" size="sm" onClick={() => setSelectedLog(null)}>
            Đóng
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
}

