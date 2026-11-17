import React from 'react';

export default function EmptyState({
  title = 'Không có dữ liệu',
  message = 'Chưa có bản ghi nào phù hợp hoặc danh sách đang trống.',
  icon = 'bi-inbox',
  actionText,
  onAction,
  bordered = false,
  className = '',
}) {
  return (
    <div
      className={`d-flex flex-column align-items-center justify-content-center text-center py-5 px-4 bg-white ${
        bordered ? 'rounded-3 border border-light-subtle' : ''
      } ${className}`}
    >
      <div
        className="d-flex align-items-center justify-content-center rounded-circle mb-3 text-secondary"
        style={{ width: '56px', height: '56px', backgroundColor: '#F1F5F9' }}
      >
        <i className={`bi ${icon} fs-3 text-muted`}></i>
      </div>
      <h6 className="fw-semibold text-dark mb-1">{title}</h6>
      <p className="text-secondary small mb-3 text-break" style={{ maxWidth: '420px' }}>
        {message}
      </p>
      {actionText && onAction && (
        <button type="button" className="btn btn-primary btn-sm d-inline-flex align-items-center gap-1" onClick={onAction}>
          <i className="bi bi-plus-lg"></i>
          <span>{actionText}</span>
        </button>
      )}
    </div>
  );
}

