import React, { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { Formik, Form as FormikForm } from 'formik';
import * as Yup from 'yup';
import Card from 'react-bootstrap/Card';
import Button from 'react-bootstrap/Button';
import Alert from 'react-bootstrap/Alert';
import Spinner from 'react-bootstrap/Spinner';
import { useAuth } from '../../auth/AuthContext';
import FormField from '../../components/common/FormField';

const LoginSchema = Yup.object().shape({
  username: Yup.string().trim().required('Vui lòng nhập tên đăng nhập'),
  password: Yup.string().required('Vui lòng nhập mật khẩu'),
});

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [generalError, setGeneralError] = useState(null);

  
  const queryParams = new URLSearchParams(location.search);
  const isSessionExpired = queryParams.get('sessionExpired') === '1';
  const returnUrlParam = queryParams.get('returnUrl');

  const getRedirectPath = (role) => {
    
    if (returnUrlParam) {
      const decoded = decodeURIComponent(returnUrlParam);
      if (decoded.startsWith('/') && !decoded.startsWith('//')) {
        return decoded;
      }
    }

    
    switch (role) {
      case 'EQUIPMENT_USER':
        return '/user/dashboard';
      case 'TECHNICIAN':
        return '/technician/dashboard';
      case 'MANAGER':
        return '/manager/dashboard';
      case 'ADMIN':
        return '/admin/dashboard';
      default:
        return '/';
    }
  };

  const handleSubmit = async (values, { setSubmitting }) => {
    setGeneralError(null);
    try {
      const user = await login(values.username.trim(), values.password);
      const redirectPath = getRedirectPath(user.role);
      navigate(redirectPath, { replace: true });
    } catch (err) {
      if (err.status === 403 || err.code === 'ACCOUNT_LOCKED') {
        
        setGeneralError(
          'Tài khoản đang bị khóa (tạm thời do nhập sai nhiều lần hoặc do Quản trị viên). Vui lòng thử lại sau hoặc liên hệ Quản trị viên.'
        );
      } else {
        
        setGeneralError('Tên đăng nhập hoặc mật khẩu không đúng');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-vh-100 d-flex align-items-center justify-content-center bg-app p-3">
      <Card className="shadow-sm border border-light-subtle" style={{ maxWidth: '420px', width: '100%' }}>
        <Card.Body className="p-4 p-sm-5">
          {}
          <div className="text-center mb-4">
            <div
              className="d-inline-flex align-items-center justify-content-center rounded-circle mb-3 text-white"
              style={{ width: '56px', height: '56px', backgroundColor: '#1D4ED8' }}
            >
              <i className="bi bi-shield-shaded fs-3"></i>
            </div>
            <h1 className="h4 fw-bold text-dark mb-1">P05 - EMS</h1>
            <p className="text-secondary small mb-0">Hệ thống Quản lý & Điều phối Bảo trì Thiết bị</p>
          </div>

          {}
          {isSessionExpired && (
            <Alert variant="warning" className="small py-2 d-flex align-items-center gap-2 mb-3">
              <i className="bi bi-info-circle-fill flex-shrink-0"></i>
              <span>Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.</span>
            </Alert>
          )}

          {}
          {generalError && (
            <Alert variant="danger" className="small py-2 d-flex align-items-center gap-2 mb-3" role="alert">
              <i className="bi bi-exclamation-triangle-fill flex-shrink-0"></i>
              <span>{generalError}</span>
            </Alert>
          )}

          {}
          <Formik
            initialValues={{ username: '', password: '' }}
            validationSchema={LoginSchema}
            onSubmit={handleSubmit}
          >
            {({ isSubmitting }) => (
              <FormikForm noValidate>
                <FormField
                  name="username"
                  label="Tên đăng nhập"
                  placeholder="Nhập tên tài khoản..."
                  required
                  autoFocus
                  autoComplete="username"
                />

                <FormField
                  name="password"
                  label="Mật khẩu"
                  type="password"
                  placeholder="Nhập mật khẩu..."
                  required
                  autoComplete="current-password"
                />

                <Button
                  type="submit"
                  variant="primary"
                  className="w-100 py-2 fw-semibold d-flex align-items-center justify-content-center gap-2 mt-4"
                  disabled={isSubmitting}
                >
                  {isSubmitting && <Spinner animation="border" size="sm" role="status" aria-hidden="true" />}
                  <span>{isSubmitting ? 'Đang xác thực...' : 'Đăng nhập hệ thống'}</span>
                </Button>
              </FormikForm>
            )}
          </Formik>

          {}
          <div className="mt-3 pt-3 border-top border-light-subtle text-center d-flex flex-column gap-2 small">
            <div>
              <span className="text-secondary">Chưa có tài khoản? </span>
              <Link to="/register" className="text-primary fw-semibold text-decoration-none">
                Đăng ký ngay
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

