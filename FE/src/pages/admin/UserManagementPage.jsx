import React, { useState, useEffect, useCallback } from 'react';
import Card from 'react-bootstrap/Card';
import Button from 'react-bootstrap/Button';
import Badge from 'react-bootstrap/Badge';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Modal from 'react-bootstrap/Modal';
import Alert from 'react-bootstrap/Alert';
import Spinner from 'react-bootstrap/Spinner';
import { useFormik } from 'formik';
import * as Yup from 'yup';
import { useAuth } from '../../auth/AuthContext';
import { usersApi } from '../../api';
import PageHeader from '../../components/common/PageHeader';
import DataTable from '../../components/common/DataTable';
import LoadingState from '../../components/common/LoadingState';
import ErrorState from '../../components/common/ErrorState';
import ConfirmDialog from '../../components/common/ConfirmDialog';

export default function UserManagementPage() {
  const { user: currentUser } = useAuth();

  const [data, setData] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // General Alert for Action Result / Errors (like CANNOT_LOCK_SELF)
  const [actionAlert, setActionAlert] = useState(null);

  // ConfirmDialog State for Approve / Reject (SCR-17)
  const [confirmState, setConfirmState] = useState({
    show: false,
    user: null,
    action: null, // 'approve' | 'reject'
    loading: false,
  });

  // Modal: Create User
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createError, setCreateError] = useState(null);

  // Modal: Change Role
  const [roleModalUser, setRoleModalUser] = useState(null);
  const [newRoleId, setNewRoleId] = useState(1);
  const [isChangingRole, setIsChangingRole] = useState(false);
  const [roleError, setRoleError] = useState(null);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = { page, size };
      if (searchQuery.trim()) params.q = searchQuery.trim();
      if (statusFilter) params.status = statusFilter;
      if (roleFilter) params.roleId = Number(roleFilter);

      const res = await usersApi.getUsers(params);
      const resData = res?.items ? res : (res?.data || {});
      setData(resData.items || []);
      setTotal(resData.total || 0);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, [page, size, searchQuery, statusFilter, roleFilter]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  // Formik for Create User
  const createFormik = useFormik({
    initialValues: {
      fullName: '',
      username: '',
      password: '',
      roleId: 3, // Default TECHNICIAN
    },
    validationSchema: Yup.object({
      fullName: Yup.string().trim().required('Vui lòng nhập họ và tên.'),
      username: Yup.string()
        .trim()
        .min(3, 'Tên đăng nhập tối thiểu 3 ký tự.')
        .max(50, 'Tên đăng nhập tối đa 50 ký tự.')
        .matches(/^[a-zA-Z0-9_]+$/, 'Tên đăng nhập chỉ chứa chữ cái, số và dấu gạch dưới.')
        .required('Vui lòng nhập tên đăng nhập.'),
      password: Yup.string()
        .min(8, 'Mật khẩu phải có ít nhất 8 ký tự.')
        .matches(/^(?=.*[A-Za-z])(?=.*\d)/, 'Mật khẩu phải chứa ít nhất một chữ cái và một chữ số.')
        .required('Vui lòng nhập mật khẩu khởi tạo.'),
      roleId: Yup.number().required('Vui lòng chọn vai trò.'),
    }),
    onSubmit: async (values, { setSubmitting, resetForm }) => {
      setCreateError(null);
      try {
        const payload = {
          fullName: values.fullName.trim(),
          username: values.username.trim(),
          password: values.password,
          roleId: Number(values.roleId),
        };
        await usersApi.createUser(payload);
        setActionAlert({
          type: 'success',
          message: `Tài khoản @${values.username} đã được tạo thành công!`,
        });
        setShowCreateModal(false);
        resetForm();
        fetchUsers();
      } catch (err) {
        if (err.status === 409 || err.code === 'USERNAME_ALREADY_EXISTS') {
          setCreateError(`Tên đăng nhập "${values.username}" đã tồn tại trên hệ thống.`);
        } else {
          setCreateError(err.message || 'Không thể tạo tài khoản mới.');
        }
      } finally {
        setSubmitting(false);
      }
    },
  });

  // Handler: Lock / Unlock User
  const handleToggleLock = async (user) => {
    const isCurrentlyActive = user.status === 'ACTIVE';
    const targetStatus = isCurrentlyActive ? 'LOCKED' : 'ACTIVE';
    const actionLabel = isCurrentlyActive ? 'khóa' : 'mở khóa';

    if (!window.confirm(`Bạn có chắc chắn muốn ${actionLabel} tài khoản @${user.username}?`)) {
      return;
    }

    setActionAlert(null);
    try {
      await usersApi.updateUser(user.userId, { status: targetStatus });
      setActionAlert({
        type: 'success',
        message: `Đã ${actionLabel} thành công tài khoản @${user.username}!`,
      });
      fetchUsers();
    } catch (err) {
      if (err.status === 403 && err.code === 'CANNOT_LOCK_SELF') {
        setActionAlert({
          type: 'danger',
          message: 'Hành động bị từ chối: Quản trị viên (Admin) không thể tự khóa tài khoản của chính mình.',
        });
      } else {
        setActionAlert({
          type: 'danger',
          message: err.message || `Không thể ${actionLabel} tài khoản @${user.username}.`,
        });
      }
    }
  };

  // Handler: Open Approve Confirm Dialog (PENDING_APPROVAL -> ACTIVE)
  const handleOpenApproveModal = (user) => {
    setConfirmState({
      show: true,
      user,
      action: 'approve',
      loading: false,
    });
  };

  // Handler: Open Reject Confirm Dialog (PENDING_APPROVAL -> REJECTED)
  const handleOpenRejectModal = (user) => {
    setConfirmState({
      show: true,
      user,
      action: 'reject',
      loading: false,
    });
  };

  // Handler: Execute Confirmation Action
  const handleConfirmAction = async () => {
    if (!confirmState.user || !confirmState.action) return;
    const { user, action } = confirmState;
    setConfirmState((prev) => ({ ...prev, loading: true }));
    setActionAlert(null);
    try {
      if (action === 'approve') {
        await usersApi.approveUser(user.userId);
        setActionAlert({
          type: 'success',
          message: `Đã phê duyệt thành công tài khoản "${user.fullName || user.username}"! Người dùng có thể đăng nhập ngay.`,
        });
      } else {
        await usersApi.rejectUser(user.userId);
        setActionAlert({
          type: 'warning',
          message: `Đã từ chối yêu cầu đăng ký của tài khoản "${user.fullName || user.username}".`,
        });
      }
      setConfirmState({ show: false, user: null, action: null, loading: false });
      fetchUsers();
    } catch (err) {
      setActionAlert({
        type: 'danger',
        message: err.message || `Thao tác không thành công đối với "${user.fullName || user.username}".`,
      });
      setConfirmState((prev) => ({ ...prev, loading: false }));
    }
  };

  // Handler: Change Role
  const handleSaveRole = async () => {
    if (!roleModalUser) return;
    setIsChangingRole(true);
    setRoleError(null);
    try {
      await usersApi.updateUser(roleModalUser.userId, { roleId: Number(newRoleId) });
      setActionAlert({
        type: 'success',
        message: `Đã thay đổi vai trò tài khoản @${roleModalUser.username} thành công!`,
      });
      setRoleModalUser(null);
      fetchUsers();
    } catch (err) {
      if (err.status === 403 && err.code === 'CANNOT_DEMOTE_SELF') {
        setRoleError('Hành động bị từ chối: Quản trị viên (Admin) không thể tự thay đổi vai trò của chính mình.');
      } else {
        setRoleError(err.message || 'Không thể cập nhật vai trò người dùng.');
      }
    } finally {
      setIsChangingRole(false);
    }
  };

  const columns = [
    {
      header: 'Họ và tên',
      field: 'fullName',
      render: (row) => (
        <div>
          <div className="fw-semibold text-dark">{row.fullName || '—'}</div>
          <div className="d-flex align-items-center gap-1">
            <span className="font-monospace text-muted small">@{row.username}</span>
            {currentUser?.userId === row.userId && (
              <Badge bg="info" className="ms-1" style={{ fontSize: '0.65rem' }}>Bạn</Badge>
            )}
          </div>
        </div>
      ),
    },
    {
      header: 'Email',
      field: 'email',
      render: (row) => (
        <span className="font-monospace text-secondary small">{row.email || '—'}</span>
      ),
    },
    {
      header: 'Vai trò (Role)',
      field: 'roleName',
      align: 'center',
      render: (row) => {
        const role = row.roleName || (row.role ? row.role.name : '—');
        const colorMap = {
          ADMIN: 'danger',
          MANAGER: 'warning',
          TECHNICIAN: 'info',
          EQUIPMENT_USER: 'secondary',
        };
        return <Badge bg={colorMap[role] || 'secondary'}>{role}</Badge>;
      },
    },
    {
      header: 'Trạng thái',
      field: 'status',
      align: 'center',
      render: (row) => {
        switch (row.status) {
          case 'ACTIVE':
            return <Badge bg="success">Hoạt động</Badge>;
          case 'LOCKED':
            return <Badge bg="danger">Đã khóa</Badge>;
          case 'PENDING_APPROVAL':
            return <Badge bg="warning" text="dark">Chờ duyệt</Badge>;
          case 'REJECTED':
            return <Badge bg="secondary">Đã từ chối</Badge>;
          default:
            return <Badge bg="light" text="dark">{row.status}</Badge>;
        }
      },
    },
    {
      header: 'Ngày đăng ký',
      field: 'createdAt',
      render: (row) => (
        <span className="font-monospace text-secondary small">
          {row.createdAt ? new Date(row.createdAt).toLocaleDateString('vi-VN') : '—'}
        </span>
      ),
    },
    {
      header: 'Số lần sai MK',
      field: 'failedLoginCount',
      align: 'center',
      render: (row) => (
        <span className={row.failedLoginCount > 0 ? 'text-danger fw-bold' : 'text-muted'}>
          {row.failedLoginCount || 0}
        </span>
      ),
    },
    {
      header: 'Thao tác',
      align: 'end',
      render: (row) => (
        <div className="d-flex justify-content-end gap-1">
          {row.status === 'PENDING_APPROVAL' ? (
            <>
              <Button
                variant="success"
                size="sm"
                className="py-1 px-2 fw-semibold d-flex align-items-center gap-1"
                onClick={() => handleOpenApproveModal(row)}
              >
                <i className="bi bi-check-circle-fill"></i>
                <span>Duyệt</span>
              </Button>
              <Button
                variant="outline-danger"
                size="sm"
                className="py-1 px-2 fw-semibold d-flex align-items-center gap-1"
                onClick={() => handleOpenRejectModal(row)}
              >
                <i className="bi bi-x-circle-fill"></i>
                <span>Từ chối</span>
              </Button>
            </>
          ) : (
            <>
              <Button
                variant="outline-secondary"
                size="sm"
                className="py-1 px-2"
                onClick={() => {
                  setRoleModalUser(row);
                  setNewRoleId(row.roleId || (row.role ? row.role.roleId : 3));
                  setRoleError(null);
                }}
              >
                Đổi vai trò
              </Button>
              {row.status !== 'REJECTED' && (
                <Button
                  variant={row.status === 'ACTIVE' ? 'outline-danger' : 'outline-success'}
                  size="sm"
                  className="py-1 px-2"
                  onClick={() => handleToggleLock(row)}
                >
                  {row.status === 'ACTIVE' ? (
                    <span><i className="bi bi-lock-fill me-1"></i>Khóa</span>
                  ) : (
                    <span><i className="bi bi-unlock-fill me-1"></i>Mở khóa</span>
                  )}
                </Button>
              )}
            </>
          )}
        </div>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Quản lý Tài khoản & Phân quyền Người dùng"
        subtitle="Quản trị viên quản lý danh sách người dùng, gán vai trò RBAC và khóa an toàn hệ thống (SCR-17)"
        actions={
          <Button
            variant="primary"
            size="sm"
            onClick={() => {
              setShowCreateModal(true);
              setCreateError(null);
              createFormik.resetForm();
            }}
            className="d-flex align-items-center gap-1"
          >
            <i className="bi bi-person-plus-fill"></i>
            <span>Thêm tài khoản mới</span>
          </Button>
        }
      />

      {/* Action Notification Alert */}
      {actionAlert && (
        <Alert
          variant={actionAlert.type}
          dismissible
          onClose={() => setActionAlert(null)}
          className="mb-4 d-flex align-items-center gap-2"
        >
          <i className={`bi ${actionAlert.type === 'danger' ? 'bi-exclamation-triangle-fill' : 'bi-check-circle-fill'} fs-5`}></i>
          <span>{actionAlert.message}</span>
        </Alert>
      )}

      {/* Filter Row */}
      <Card className="border border-light-subtle shadow-sm mb-4">
        <Card.Body className="p-3">
          <Row className="g-3 align-items-end">
            <Col xs={12} sm={6} md={4}>
              <Form.Label className="small fw-semibold text-secondary">Tìm kiếm theo tên / Username</Form.Label>
              <Form.Control
                type="text"
                size="sm"
                placeholder="Nhập username hoặc tên nhân viên..."
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  setPage(0);
                }}
              />
            </Col>

            <Col xs={12} sm={6} md={3}>
              <Form.Label className="small fw-semibold text-secondary">Vai trò (Role)</Form.Label>
              <Form.Select
                size="sm"
                value={roleFilter}
                onChange={(e) => {
                  setRoleFilter(e.target.value);
                  setPage(0);
                }}
              >
                <option value="">Tất cả vai trò</option>
                <option value="1">ADMIN — Quản trị viên</option>
                <option value="2">MANAGER — Quản lý bảo trì</option>
                <option value="3">TECHNICIAN — Kỹ thuật viên</option>
                <option value="4">EQUIPMENT_USER — Người dùng thiết bị</option>
              </Form.Select>
            </Col>

            <Col xs={12} sm={6} md={3}>
              <Form.Label className="small fw-semibold text-secondary">Trạng thái tài khoản</Form.Label>
              <Form.Select
                size="sm"
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value);
                  setPage(0);
                }}
              >
                <option value="">Tất cả trạng thái</option>
                <option value="ACTIVE">Hoạt động (ACTIVE)</option>
                <option value="PENDING_APPROVAL">Chờ duyệt (PENDING_APPROVAL)</option>
                <option value="LOCKED">Đã khóa (LOCKED)</option>
                <option value="REJECTED">Đã từ chối (REJECTED)</option>
              </Form.Select>
            </Col>

            <Col xs={12} sm={6} md={2}>
              <Button
                variant="outline-secondary"
                size="sm"
                className="w-100"
                onClick={() => {
                  setSearchQuery('');
                  setRoleFilter('');
                  setStatusFilter('');
                  setPage(0);
                }}
              >
                Đặt lại
              </Button>
            </Col>
          </Row>
        </Card.Body>
      </Card>

      {/* Users Table */}
      {loading ? (
        <LoadingState message="Đang nạp danh sách tài khoản..." />
      ) : error ? (
        <ErrorState
          title="Không thể tải danh sách tài khoản"
          message={error.message}
          code={error.code}
          onRetry={fetchUsers}
        />
      ) : (
        <DataTable
          columns={columns}
          data={data}
          keyField="userId"
          emptyTitle="Không có tài khoản nào"
          emptyMessage="Không tìm thấy người dùng nào phù hợp với bộ lọc tìm kiếm."
          pagination={{
            page,
            size,
            total,
            onPageChange: (newPage) => setPage(newPage),
            onSizeChange: (newSize) => {
              setSize(newSize);
              setPage(0);
            },
          }}
        />
      )}

      {/* Modal: Create User */}
      <Modal show={showCreateModal} onHide={() => setShowCreateModal(false)} centered>
        <Modal.Header closeButton className="bg-light border-bottom">
          <Modal.Title className="h6 fw-bold">
            <i className="bi bi-person-plus-fill me-2 text-primary"></i>
            Tạo Tài khoản Người dùng Mới
          </Modal.Title>
        </Modal.Header>
        <Form onSubmit={createFormik.handleSubmit}>
          <Modal.Body className="p-4">
            {createError && (
              <Alert variant="danger" className="small py-2 border-danger mb-3">
                <i className="bi bi-exclamation-triangle-fill me-1"></i>
                {createError}
              </Alert>
            )}

            <Form.Group className="mb-3" controlId="createFullName">
              <Form.Label className="small fw-semibold">
                Họ và tên <span className="text-danger">*</span>
              </Form.Label>
              <Form.Control
                type="text"
                name="fullName"
                placeholder="Ví dụ: Nguyễn Văn An"
                value={createFormik.values.fullName}
                onChange={createFormik.handleChange}
                onBlur={createFormik.handleBlur}
                isInvalid={createFormik.touched.fullName && Boolean(createFormik.errors.fullName)}
              />
              <Form.Control.Feedback type="invalid">
                {createFormik.errors.fullName}
              </Form.Control.Feedback>
            </Form.Group>

            <Form.Group className="mb-3" controlId="createUsername">
              <Form.Label className="small fw-semibold">
                Tên đăng nhập (Username) <span className="text-danger">*</span>
              </Form.Label>
              <Form.Control
                type="text"
                name="username"
                placeholder="Ví dụ: TECH_C"
                value={createFormik.values.username}
                onChange={createFormik.handleChange}
                onBlur={createFormik.handleBlur}
                isInvalid={createFormik.touched.username && Boolean(createFormik.errors.username)}
              />
              <Form.Control.Feedback type="invalid">
                {createFormik.errors.username}
              </Form.Control.Feedback>
            </Form.Group>

            <Form.Group className="mb-3" controlId="createRoleId">
              <Form.Label className="small fw-semibold">
                Vai trò hệ thống (Role) <span className="text-danger">*</span>
              </Form.Label>
              <Form.Select
                name="roleId"
                value={createFormik.values.roleId}
                onChange={createFormik.handleChange}
                onBlur={createFormik.handleBlur}
              >
                <option value={3}>TECHNICIAN — Kỹ thuật viên bảo trì</option>
                <option value={2}>MANAGER — Quản lý thiết bị</option>
                <option value={4}>EQUIPMENT_USER — Người dùng phòng lab</option>
                <option value={1}>ADMIN — Quản trị viên hệ thống</option>
              </Form.Select>
            </Form.Group>

            <Form.Group className="mb-3" controlId="createPassword">
              <Form.Label className="small fw-semibold">
                Mật khẩu khởi tạo <span className="text-danger">*</span>
              </Form.Label>
              <Form.Control
                type="password"
                name="password"
                autoComplete="new-password"
                placeholder="Tối thiểu 8 ký tự, gồm chữ và số"
                value={createFormik.values.password}
                onChange={createFormik.handleChange}
                onBlur={createFormik.handleBlur}
                isInvalid={createFormik.touched.password && Boolean(createFormik.errors.password)}
              />
              <Form.Control.Feedback type="invalid">
                {createFormik.errors.password}
              </Form.Control.Feedback>
              <Form.Text className="text-muted small">
                Yêu cầu mật khẩu mạnh: tối thiểu 8 ký tự, bao gồm ít nhất một chữ cái và một chữ số.
              </Form.Text>
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" size="sm" onClick={() => setShowCreateModal(false)}>
              Hủy bỏ
            </Button>
            <Button
              variant="primary"
              size="sm"
              type="submit"
              disabled={createFormik.isSubmitting}
              className="d-flex align-items-center gap-2"
            >
              {createFormik.isSubmitting && <Spinner size="sm" animation="border" />}
              <span>Tạo tài khoản</span>
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* Modal: Change Role */}
      <Modal show={Boolean(roleModalUser)} onHide={() => setRoleModalUser(null)} centered>
        <Modal.Header closeButton className="bg-light border-bottom">
          <Modal.Title className="h6 fw-bold">
            <i className="bi bi-shield-lock me-2 text-primary"></i>
            Thay Đổi Vai Trò Người Dùng
          </Modal.Title>
        </Modal.Header>
        <Modal.Body className="p-4">
          {roleError && (
            <Alert variant="danger" className="small py-2 border-danger mb-3">
              <i className="bi bi-exclamation-triangle-fill me-1"></i>
              {roleError}
            </Alert>
          )}

          <p className="small mb-3">
            Đang thay đổi vai trò cho tài khoản: <strong>@{roleModalUser?.username}</strong> ({roleModalUser?.fullName}).
          </p>

          <Form.Group controlId="modalRoleId">
            <Form.Label className="small fw-semibold">Chọn vai trò mới</Form.Label>
            <Form.Select
              value={newRoleId}
              onChange={(e) => setNewRoleId(e.target.value)}
            >
              <option value={1}>ADMIN — Quản trị viên</option>
              <option value={2}>MANAGER — Quản lý thiết bị</option>
              <option value={3}>TECHNICIAN — Kỹ thuật viên</option>
              <option value={4}>EQUIPMENT_USER — Người dùng phòng lab</option>
            </Form.Select>
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" size="sm" onClick={() => setRoleModalUser(null)}>
            Hủy bỏ
          </Button>
          <Button
            variant="primary"
            size="sm"
            onClick={handleSaveRole}
            disabled={isChangingRole}
            className="d-flex align-items-center gap-2"
          >
            {isChangingRole && <Spinner size="sm" animation="border" />}
            <span>Lưu thay đổi vai trò</span>
          </Button>
        </Modal.Footer>
      </Modal>

      {/* ConfirmDialog for Approve / Reject (SCR-17) */}
      <ConfirmDialog
        show={confirmState.show}
        onHide={() => !confirmState.loading && setConfirmState({ show: false, user: null, action: null, loading: false })}
        onConfirm={handleConfirmAction}
        isLoading={confirmState.loading}
        title={confirmState.action === 'approve' ? 'Phê duyệt tài khoản người dùng' : 'Từ chối yêu cầu đăng ký'}
        message={
          confirmState.action === 'approve'
            ? `Bạn có chắc chắn muốn phê duyệt tài khoản cho "${confirmState.user?.fullName || confirmState.user?.username}" (${confirmState.user?.email || 'Chưa có email'})? Người dùng sẽ được kích hoạt quyền truy cập ngay lập tức.`
            : `Bạn có chắc chắn muốn từ chối yêu cầu đăng ký của "${confirmState.user?.fullName || confirmState.user?.username}" (${confirmState.user?.email || 'Chưa có email'})? Quyết định từ chối này sẽ được ghi nhận và người dùng sẽ không thể đăng nhập.`
        }
        confirmText={confirmState.action === 'approve' ? 'Xác nhận duyệt' : 'Xác nhận từ chối'}
        variant={confirmState.action === 'approve' ? 'success' : 'danger'}
      />
    </div>
  );
}
