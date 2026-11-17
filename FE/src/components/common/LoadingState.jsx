import React from 'react';
import Spinner from 'react-bootstrap/Spinner';

export default function LoadingState({ message = 'Đang tải dữ liệu...', fullPage = false, className = '' }) {
  const content = (
    <div className={`d-flex flex-column align-items-center justify-content-center p-5 ${className}`}>
      <Spinner animation="border" variant="primary" role="status" className="mb-3" style={{ width: '2.5rem', height: '2.5rem' }}>
        <span className="visually-hidden">Loading...</span>
      </Spinner>
      <div className="text-secondary small fw-medium">{message}</div>
    </div>
  );

  if (fullPage) {
    return (
      <div className="min-vh-100 d-flex align-items-center justify-content-center bg-app">
        {content}
      </div>
    );
  }

  return content;
}

