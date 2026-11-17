import React from 'react';
import Card from 'react-bootstrap/Card';

export default function FilterBar({
  children,
  onReset,
  actions,
  className = 'mb-4',
}) {
  return (
    <Card className={`border border-light-subtle shadow-sm ${className}`}>
      <Card.Body className="p-3">
        <div className="d-flex flex-column flex-lg-row align-items-lg-center justify-content-between gap-3">
          <div className="d-flex flex-wrap align-items-center gap-2 flex-grow-1">
            {children}
            {onReset && (
              <button
                type="button"
                className="btn btn-outline-secondary btn-sm d-inline-flex align-items-center gap-1"
                onClick={onReset}
                title="Đặt lại bộ lọc"
              >
                <i className="bi bi-arrow-counterclockwise"></i>
                <span>Đặt lại</span>
              </button>
            )}
          </div>
          {actions && <div className="d-flex align-items-center gap-2 flex-shrink-0">{actions}</div>}
        </div>
      </Card.Body>
    </Card>
  );
}

