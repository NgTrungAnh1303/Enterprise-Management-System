import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Formik, Form as FormikForm, Field, ErrorMessage } from 'formik';
import * as Yup from 'yup';
import Card from 'react-bootstrap/Card';
import Button from 'react-bootstrap/Button';
import Alert from 'react-bootstrap/Alert';
import Spinner from 'react-bootstrap/Spinner';
import Form from 'react-bootstrap/Form';
import InputGroup from 'react-bootstrap/InputGroup';
import { authApi } from '../../api';

const RegisterSchema = Yup.object().shape({
  fullName: Yup.string()
    .trim()
    .max(100, 'Họ và tên tối đa 100 ký tự')
    .required('Vui lòng nhập họ và tên'),
  email: Yup.string()
    .trim()
    .email('Địa chỉ email không đúng định dạng')
    .max(100, 'Email tối đa 100 ký tự')
    .required('Vui lòng nhập email'),
  password: Yup.string()
    .min(12, 'Mật khẩu phải có tối thiểu 12 ký tự')
    .matches(/[A-Z]/, 'Mật khẩu phải chứa ít nhất 1 chữ hoa')
    .matches(/[a-z]/, 'Mật khẩu phải chứa ít nhất 1 chữ thường')
    .matches(/[0-9]/, 'Mật khẩu phải chứa ít nhất 1 chữ số')
    .required('Vui lòng nhập mật khẩu'),
  confirmPassword: Yup.string()
    .oneOf([Yup.ref('password'), null], 'Mật khẩu xác nhận không khớp')
    .required('Vui lòng xác nhận lại mật khẩu'),
});

export default function RegisterPage() {
  const navigate = useNavigate();

  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [successInfo, setSuccessInfo] = useState(null);
  const [generalError, setGeneralError] = useState(null);

  const handleSubmit = async (values, { setSubmitting, setErrors }) => {
    setGeneralError(null);
    setSuccessInfo(null);

    try {
      const payload = {
        fullName: values.fullName.trim(),
        email: values.email.trim().toLowerCase(),
        password: values.password,
        confirmPassword: values.confirmPassword,
      };

      const res = await authApi.register(payload);
      setSuccessInfo(res.message || 'Yêu cầu đăng ký đã được ghi nhận. Vui lòng chờ quản trị viên phê duyệt trước khi đăng nhập.');
    } catch (err) {
      if (err.status === 429) {
        setGeneralError('Bạn thao tác quá nhiều lần. Vui lòng thử lại sau 1 giờ.');
      } else if (err.status === 400) {
        if (err.fieldErrors && Object.keys(err.fieldErrors).length > 0) {
          setErrors(err.fieldErrors);
        } else {
          setGeneralError(err.message || 'Dữ liệu đăng ký không hợp lệ hoặc mật khẩu chưa đạt yêu cầu an toàn.');
        }
      } else {
        setGeneralError(err.message || 'Đã có lỗi xảy ra trong quá trình đăng ký. Vui lòng thử lại.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-vh-100 d-flex align-items-center justify-content-center bg-app p-3 py-5">
      <Card className="shadow-sm border border-light-subtle" style={{ maxWidth: '480px', width: '100%' }}>
        <Card.Body className="p-4 p-sm-5">
          {/* Brand Header */}
          <div className="text-center mb-4">
            <div
              className="d-inline-flex align-items-center justify-content-center rounded-circle mb-3 text-white"
              style={{ width: '56px', height: '56px', backgroundColor: '#1D4ED8' }}
            >
              <i className="bi bi-person-plus-fill fs-3"></i>
            </div>
            <h1 className="h4 fw-bold text-dark mb-1">Đăng ký Tài khoản</h1>
            <p className="text-secondary small mb-0">Hệ thống Quản lý & Điều phối Bảo trì Thiết bị</p>
          </div>

          {/* Success Notification Alert */}
          {successInfo && (
            <Alert variant="success" className="small py-3 mb-4 d-flex flex-column gap-2" role="alert">
              <div className="d-flex align-items-center gap-2">
                <i className="bi bi-check-circle-fill fs-5 flex-shrink-0"></i>
                <span className="fw-semibold">{successInfo}</span>
              </div>
              <div className="mt-2 text-end">
                <Button variant="success" size="sm" onClick={() => navigate('/login')} className="fw-semibold">
                  Đến trang Đăng nhập
                </Button>
              </div>
            </Alert>
          )}

          {/* General Error Notice */}
          {generalError && (
            <Alert variant="danger" className="small py-2 d-flex align-items-center gap-2 mb-3" role="alert">
              <i className="bi bi-exclamation-triangle-fill flex-shrink-0"></i>
              <span>{generalError}</span>
            </Alert>
          )}

          {/* Registration Form (hidden when registration succeeds) */}
          {!successInfo && (
            <Formik
              initialValues={{ fullName: '', email: '', password: '', confirmPassword: '' }}
              validationSchema={RegisterSchema}
              onSubmit={handleSubmit}
            >
              {({ isSubmitting, errors, touched }) => (
                <FormikForm noValidate>
                  {/* Full Name */}
                  <Form.Group className="mb-3" controlId="registerFullName">
                    <Form.Label className="small fw-semibold text-secondary mb-1">
                      Họ và tên <span className="text-danger">*</span>
                    </Form.Label>
                    <Field
                      as={Form.Control}
                      name="fullName"
                      type="text"
                      placeholder="Ví dụ: Nguyễn Văn An"
                      isInvalid={touched.fullName && Boolean(errors.fullName)}
                      aria-describedby="fullNameError"
                      autoFocus
                    />
                    <ErrorMessage name="fullName" component={Form.Control.Feedback} type="invalid" id="fullNameError" />
                  </Form.Group>

                  {/* Email */}
                  <Form.Group className="mb-3" controlId="registerEmail">
                    <Form.Label className="small fw-semibold text-secondary mb-1">
                      Địa chỉ Email <span className="text-danger">*</span>
                    </Form.Label>
                    <Field
                      as={Form.Control}
                      name="email"
                      type="email"
                      placeholder="name@example.com"
                      isInvalid={touched.email && Boolean(errors.email)}
                      aria-describedby="emailError"
                    />
                    <ErrorMessage name="email" component={Form.Control.Feedback} type="invalid" id="emailError" />
                  </Form.Group>

                  {/* Password */}
                  <Form.Group className="mb-2" controlId="registerPassword">
                    <Form.Label className="small fw-semibold text-secondary mb-1">
                      Mật khẩu <span className="text-danger">*</span>
                    </Form.Label>
                    <InputGroup hasValidation>
                      <Field
                        as={Form.Control}
                        name="password"
                        type={showPassword ? 'text' : 'password'}
                        placeholder="Tối thiểu 12 ký tự..."
                        isInvalid={touched.password && Boolean(errors.password)}
                        aria-describedby="passwordHelp passwordError"
                        autoComplete="new-password"
                      />
                      <Button
                        variant="outline-secondary"
                        onClick={() => setShowPassword((prev) => !prev)}
                        aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                        type="button"
                      >
                        <i className={`bi ${showPassword ? 'bi-eye-slash-fill' : 'bi-eye-fill'}`}></i>
                      </Button>
                      <ErrorMessage name="password" component={Form.Control.Feedback} type="invalid" id="passwordError" />
                    </InputGroup>
                    <Form.Text id="passwordHelp" className="text-muted small" style={{ fontSize: '0.75rem' }}>
                      <i className="bi bi-shield-lock me-1"></i>
                      Mật khẩu tối thiểu 12 ký tự, bao gồm chữ hoa, chữ thường và chữ số; không chứa họ tên hoặc email.
                    </Form.Text>
                  </Form.Group>

                  {/* Confirm Password */}
                  <Form.Group className="mb-4 mt-3" controlId="registerConfirmPassword">
                    <Form.Label className="small fw-semibold text-secondary mb-1">
                      Xác nhận mật khẩu <span className="text-danger">*</span>
                    </Form.Label>
                    <InputGroup hasValidation>
                      <Field
                        as={Form.Control}
                        name="confirmPassword"
                        type={showConfirmPassword ? 'text' : 'password'}
                        placeholder="Nhập lại mật khẩu..."
                        isInvalid={touched.confirmPassword && Boolean(errors.confirmPassword)}
                        aria-describedby="confirmPasswordError"
                        autoComplete="new-password"
                      />
                      <Button
                        variant="outline-secondary"
                        onClick={() => setShowConfirmPassword((prev) => !prev)}
                        aria-label={showConfirmPassword ? 'Ẩn xác nhận mật khẩu' : 'Hiện xác nhận mật khẩu'}
                        type="button"
                      >
                        <i className={`bi ${showConfirmPassword ? 'bi-eye-slash-fill' : 'bi-eye-fill'}`}></i>
                      </Button>
                      <ErrorMessage
                        name="confirmPassword"
                        component={Form.Control.Feedback}
                        type="invalid"
                        id="confirmPasswordError"
                      />
                    </InputGroup>
                  </Form.Group>

                  {/* Submit Button */}
                  <Button
                    type="submit"
                    variant="primary"
                    className="w-100 py-2 fw-semibold d-flex align-items-center justify-content-center gap-2 mb-3"
                    disabled={isSubmitting}
                  >
                    {isSubmitting && <Spinner animation="border" size="sm" role="status" aria-hidden="true" />}
                    <span>{isSubmitting ? 'Đang gửi thông tin...' : 'Đăng ký tài khoản'}</span>
                  </Button>
                </FormikForm>
              )}
            </Formik>
          )}

          {/* Navigation Links Footer */}
          <div className="mt-3 pt-3 border-top border-light-subtle text-center d-flex flex-column gap-2 small">
            <div>
              <span className="text-secondary">Đã có tài khoản? </span>
              <Link to="/login" className="text-primary fw-semibold text-decoration-none">
                Đăng nhập ngay
              </Link>
            </div>
            <div>
              <Link to="/" className="text-muted text-decoration-none small">
                <i className="bi bi-arrow-left me-1"></i>
                Về trang chủ
              </Link>
            </div>
          </div>
        </Card.Body>
      </Card>
    </div>
  );
}
