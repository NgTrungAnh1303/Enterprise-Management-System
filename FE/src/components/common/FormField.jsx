import React from 'react';
import { useField } from 'formik';
import Form from 'react-bootstrap/Form';

export default function FormField({
  name,
  label,
  type = 'text',
  placeholder,
  required = false,
  disabled = false,
  serverError = null,
  helpText = null,
  rows = 3,
  children,
  className = 'mb-3',
  ...props
}) {
  const [field, meta] = useField(name);
  const isInvalid = Boolean(meta.touched && (meta.error || serverError));
  const errorMessage = (meta.touched && meta.error) || serverError;

  return (
    <Form.Group className={className} controlId={`field-${name}`}>
      {label && (
        <Form.Label className="fw-semibold small text-dark mb-1">
          {label} {required && <span className="text-danger">*</span>}
        </Form.Label>
      )}

      {type === 'select' ? (
        <Form.Select
          {...field}
          {...props}
          disabled={disabled}
          isInvalid={isInvalid}
          className="form-control-sm"
        >
          {children}
        </Form.Select>
      ) : type === 'textarea' ? (
        <Form.Control
          {...field}
          {...props}
          as="textarea"
          rows={rows}
          placeholder={placeholder}
          disabled={disabled}
          isInvalid={isInvalid}
          className="form-control-sm"
        />
      ) : (
        <Form.Control
          {...field}
          {...props}
          type={type}
          placeholder={placeholder}
          disabled={disabled}
          isInvalid={isInvalid}
          className="form-control-sm"
        />
      )}

      {helpText && !isInvalid && (
        <Form.Text className="text-muted small" style={{ fontSize: '0.75rem' }}>
          {helpText}
        </Form.Text>
      )}

      {isInvalid && (
        <Form.Control.Feedback type="invalid" className="small" style={{ fontSize: '0.75rem' }}>
          {errorMessage}
        </Form.Control.Feedback>
      )}
    </Form.Group>
  );
}

