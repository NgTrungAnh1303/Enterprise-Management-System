import React from 'react';
import Toast from 'react-bootstrap/Toast';
import ToastContainer from 'react-bootstrap/ToastContainer';

export default function ToastAlert({
  show = false,
  onClose,
  title = 'Thông báo',
  message = '',
  variant = 'success',
  delay = 3000,
  autohide = true,
}) {
  const getIcon = () => {
    switch (variant) {
      case 'success':
        return 'bi-check-circle-fill text-success';
      case 'danger':
        return 'bi-exclamation-octagon-fill text-danger';
      case 'warning':
        return 'bi-exclamation-triangle-fill text-warning';
      default:
        return 'bi-info-circle-fill text-primary';
    }
  };

  return (
    <ToastContainer position="top-end" className="p-3" style={{ zIndex: 1090 }}>
      <Toast show={show} onClose={onClose} delay={delay} autohide={autohide} className="shadow">
        <Toast.Header closeButton>
          <i className={`bi ${getIcon()} me-2`}></i>
          <strong className="me-auto small">{title}</strong>
        </Toast.Header>
        <Toast.Body className="small text-secondary">{message}</Toast.Body>
      </Toast>
    </ToastContainer>
  );
}

