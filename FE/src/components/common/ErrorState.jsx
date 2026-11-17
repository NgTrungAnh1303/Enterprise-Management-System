import React from 'react';

export default function ErrorState({
  title = 'Đã xảy ra lỗi khi tải dữ liệu',
  message = 'Không thể nạp thông tin từ máy chủ. Vui lòng thử lại sau.',
  code,
  requestId,
  onRetry,
  compact = false,
  className = '',
}) {
  if (compact) {
    return (
      <div
        className={`alert d-flex align-items-center justify-content-between p-3 rounded-3 ${className}`}
        style={{
          color: '#991B1B',
          backgroundColor: '#FEF2F2',
          border: '1px solid #F87171',
        }}
        role="alert"
      >
        <div className="d-flex align-items-center gap-2">
          <i className="bi bi-exclamation-octagon-fill fs-5 flex-shrink-0"></i>
          <div>
            <div className="fw-semibold small">{message}</div>
            {(code || requestId) && (
              <div className="text-secondary small" style={{ fontSize: '0.75rem' }}>
                {code && <span className="me-2">Mã lỗi: <code>{code}</code></span>}
                {requestId && <span>Yêu cầu: <code>{requestId}</code></span>}
              </div>
            )}
          </div>
        </div>
        {onRetry && (
          <button
            type="button"
            className="btn btn-sm btn-outline-danger ms-3 flex-shrink-0"
            onClick={onRetry}
          >
            <i className="bi bi-arrow-clockwise me-1"></i>
            Thử lại
          </button>
        )}
      </div>
    );
  }

  return (
    <div
      className={`d-flex flex-column align-items-center justify-content-center text-center p-5 rounded-3 border ${className}`}
      style={{
        backgroundColor: '#FEF2F2',
        borderColor: '#F87171',
        color: '#991B1B',
      }}
      role="alert"
    >
      <div
        className="d-flex align-items-center justify-content-center rounded-circle mb-3"
        style={{ width: '60px', height: '60px', backgroundColor: '#FEE2E2' }}
      >
        <i className="bi bi-exclamation-triangle-fill fs-3 text-danger"></i>
      </div>
      <h6 className="fw-bold mb-2">{title}</h6>
      <p className="small mb-3 text-break" style={{ maxWidth: '480px', color: '#7F1D1D' }}>
        {message}
      </p>

      {(code || requestId) && (
        <div className="mb-3 px-3 py-1 rounded bg-white border border-danger-subtle small text-secondary">
          {code && <span className="me-3">Mã lỗi: <strong className="text-danger">{code}</strong></span>}
          {requestId && <span>Mã đối soát (Request ID): <code>{requestId}</code></span>}
        </div>
      )}

      {onRetry && (
        <button
          type="button"
          className="btn btn-danger btn-sm d-inline-flex align-items-center gap-1"
          onClick={onRetry}
        >
          <i className="bi bi-arrow-clockwise"></i>
          <span>Tải lại dữ liệu</span>
        </button>
      )}
    </div>
  );
}

