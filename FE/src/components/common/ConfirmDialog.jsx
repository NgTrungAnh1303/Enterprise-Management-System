import React from 'react';
import Modal from 'react-bootstrap/Modal';
import Button from 'react-bootstrap/Button';
import Spinner from 'react-bootstrap/Spinner';

export default function ConfirmDialog({
  show = false,
  onHide,
  onConfirm,
  title = 'Xác nhận thao tác',
  message = 'Bạn có chắc chắn muốn thực hiện hành động này?',
  confirmText = 'Xác nhận',
  cancelText = 'Hủy bỏ',
  variant = 'primary',
  isLoading = false,
}) {
  return (
    <Modal show={show} onHide={isLoading ? undefined : onHide} centered backdrop={isLoading ? 'static' : true}>
      <Modal.Header closeButton={!isLoading} className="border-bottom border-light-subtle">
        <Modal.Title className="fs-6 fw-bold text-dark d-flex align-items-center gap-2">
          {variant === 'danger' ? (
            <i className="bi bi-exclamation-triangle-fill text-danger"></i>
          ) : (
            <i className="bi bi-question-circle-fill text-primary"></i>
          )}
          <span>{title}</span>
        </Modal.Title>
      </Modal.Header>
      <Modal.Body className="py-4">
        <p className="text-secondary mb-0 small">{message}</p>
      </Modal.Body>
      <Modal.Footer className="border-top border-light-subtle py-2">
        <Button variant="outline-secondary" size="sm" onClick={onHide} disabled={isLoading}>
          {cancelText}
        </Button>
        <Button
          variant={variant}
          size="sm"
          onClick={onConfirm}
          disabled={isLoading}
          className="d-inline-flex align-items-center gap-1"
        >
          {isLoading && <Spinner animation="border" size="sm" role="status" aria-hidden="true" />}
          <span>{confirmText}</span>
        </Button>
      </Modal.Footer>
    </Modal>
  );
}

