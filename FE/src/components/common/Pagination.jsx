import React from 'react';
import Form from 'react-bootstrap/Form';

export default function Pagination({
  page = 0,
  size = 20,
  total = 0,
  onPageChange,
  onSizeChange,
  sizeOptions = [10, 20, 50, 100],
}) {
  const totalPages = Math.ceil(total / size) || 1;
  const startItem = total === 0 ? 0 : page * size + 1;
  const endItem = Math.min((page + 1) * size, total);

  const handlePrev = () => {
    if (page > 0) onPageChange(page - 1);
  };

  const handleNext = () => {
    if (page < totalPages - 1) onPageChange(page + 1);
  };

  const handleFirst = () => {
    if (page !== 0) onPageChange(0);
  };

  const handleLast = () => {
    if (page !== totalPages - 1) onPageChange(totalPages - 1);
  };

  return (
    <div className="d-flex flex-column flex-sm-row align-items-center justify-content-between gap-3 p-3 bg-white border-top border-light-subtle">
      {/* Summary count */}
      <div className="text-secondary small">
        Hiển thị <span className="fw-semibold text-dark">{startItem}</span>–
        <span className="fw-semibold text-dark">{endItem}</span> trong tổng số{' '}
        <span className="fw-semibold text-dark">{total}</span> bản ghi
      </div>

      {/* Controls: Page size selector + Navigation buttons */}
      <div className="d-flex align-items-center gap-3">
        {onSizeChange && (
          <div className="d-flex align-items-center gap-2">
            <span className="text-secondary small text-nowrap">Số hàng:</span>
            <Form.Select
              size="sm"
              value={size}
              onChange={(e) => onSizeChange(Number(e.target.value))}
              style={{ width: '75px' }}
              aria-label="Chọn số bản ghi trên một trang"
            >
              {sizeOptions.map((opt) => (
                <option key={opt} value={opt}>
                  {opt}
                </option>
              ))}
            </Form.Select>
          </div>
        )}

        <div className="btn-group btn-group-sm" role="group" aria-label="Điều hướng phân trang">
          <button
            type="button"
            className="btn btn-outline-secondary"
            onClick={handleFirst}
            disabled={page <= 0}
            title="Trang đầu"
          >
            <i className="bi bi-chevron-double-left"></i>
          </button>
          <button
            type="button"
            className="btn btn-outline-secondary"
            onClick={handlePrev}
            disabled={page <= 0}
            title="Trang trước"
          >
            <i className="bi bi-chevron-left"></i>
          </button>

          <span className="btn btn-outline-secondary disabled text-dark fw-semibold px-3">
            {page + 1} / {totalPages}
          </span>

          <button
            type="button"
            className="btn btn-outline-secondary"
            onClick={handleNext}
            disabled={page >= totalPages - 1}
            title="Trang sau"
          >
            <i className="bi bi-chevron-right"></i>
          </button>
          <button
            type="button"
            className="btn btn-outline-secondary"
            onClick={handleLast}
            disabled={page >= totalPages - 1}
            title="Trang cuối"
          >
            <i className="bi bi-chevron-double-right"></i>
          </button>
        </div>
      </div>
    </div>
  );
}

