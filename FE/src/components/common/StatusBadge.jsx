import React from 'react';

const BADGE_CONFIG = {
  role: {
    EQUIPMENT_USER: { label: 'Người dùng', color: '#1E40AF', bg: '#DBEAFE', border: '#93C5FD', icon: 'bi-person' },
    TECHNICIAN: { label: 'Kỹ thuật viên', color: '#5B21B6', bg: '#EDE9FE', border: '#C4B5FD', icon: 'bi-tools' },
    MANAGER: { label: 'Quản lý', color: '#92400E', bg: '#FEF3C7', border: '#FDE68A', icon: 'bi-shield-shaded' },
    ADMIN: { label: 'Quản trị viên', color: '#B91C1C', bg: '#FEE2E2', border: '#FCA5A5', icon: 'bi-gear-fill' },
  },
  equipment: {
    ACTIVE: { label: 'Hoạt động', color: '#15803D', bg: '#DCFCE7', border: '#86EFAC', icon: 'bi-check-circle-fill' },
    UNDER_MAINTENANCE: { label: 'Đang bảo dưỡng', color: '#92400E', bg: '#FEF3C7', border: '#FDE68A', icon: 'bi-wrench' },
    OUT_OF_SERVICE: { label: 'Ngừng hoạt động', color: '#B91C1C', bg: '#FEE2E2', border: '#FCA5A5', icon: 'bi-slash-circle-fill' },
  },
  due: {
    DUE_SOON: { label: 'Đến hạn', color: '#9A3412', bg: '#FFEDD5', border: '#FDBA74', icon: 'bi-clock-history' },
    OVERDUE: { label: 'Quá hạn', color: '#991B1B', bg: '#FFE4E6', border: '#FDA4AF', icon: 'bi-exclamation-triangle-fill' },
    NORMAL: { label: 'Còn hạn', color: '#475569', bg: '#F1F5F9', border: '#CBD5E1', icon: 'bi-calendar-check' },
  },
  workOrder: {
    OPEN: { label: 'Mới tạo', color: '#1E40AF', bg: '#DBEAFE', border: '#93C5FD', icon: 'bi-plus-circle' },
    ASSIGNED: { label: 'Đã phân công', color: '#5B21B6', bg: '#EDE9FE', border: '#C4B5FD', icon: 'bi-person-check' },
    IN_PROGRESS: { label: 'Đang thực hiện', color: '#92400E', bg: '#FEF3C7', border: '#FDE68A', icon: 'bi-play-circle-fill' },
    SUBMITTED: { label: 'Chờ nghiệm thu', color: '#075985', bg: '#E0F2FE', border: '#7DD3FC', icon: 'bi-send-check' },
    CLOSED: { label: 'Đã đóng', color: '#15803D', bg: '#DCFCE7', border: '#86EFAC', icon: 'bi-lock-fill' },
  },
  incident: {
    REPORTED: { label: 'Mới báo', color: '#92400E', bg: '#FEF3C7', border: '#FDE68A', icon: 'bi-envelope-exclamation' },
    CLASSIFIED: { label: 'Đã phân loại', color: '#5B21B6', bg: '#EDE9FE', border: '#C4B5FD', icon: 'bi-tag-fill' },
    ASSIGNED: { label: 'Đã phân công', color: '#1E40AF', bg: '#DBEAFE', border: '#93C5FD', icon: 'bi-person-gear' },
    IN_REPAIR: { label: 'Đang sửa chữa', color: '#92400E', bg: '#FEF3C7', border: '#FDE68A', icon: 'bi-wrench-adjustable' },
    RESOLVED: { label: 'Đã xử lý', color: '#0F766E', bg: '#CCFBF1', border: '#5EEAD4', icon: 'bi-check2-all' },
    CLOSED: { label: 'Đã đóng', color: '#334155', bg: '#F1F5F9', border: '#CBD5E1', icon: 'bi-archive-fill' },
  },
  severity: {
    LOW: { label: 'Thấp', color: '#075985', bg: '#E0F2FE', border: '#7DD3FC' },
    MEDIUM: { label: 'Vừa', color: '#92400E', bg: '#FEF3C7', border: '#FDE68A' },
    HIGH: { label: 'Cao', color: '#B91C1C', bg: '#FEE2E2', border: '#FCA5A5' },
    CRITICAL: { label: 'Nghiêm trọng', color: '#881337', bg: '#FFE4E6', border: '#FDA4AF', icon: 'bi-fire' },
  },
  userStatus: {
    ACTIVE: { label: 'Hoạt động', color: '#15803D', bg: '#DCFCE7', border: '#86EFAC' },
    LOCKED: { label: 'Đã khóa', color: '#B91C1C', bg: '#FEE2E2', border: '#FCA5A5', icon: 'bi-lock' },
  },
};

export default function StatusBadge({ type = 'equipment', status, label, showIcon = true, className = '' }) {
  if (!status) return null;

  const typeConfig = BADGE_CONFIG[type] || {};
  const config = typeConfig[status] || {
    label: label || status,
    color: '#475569',
    bg: '#F1F5F9',
    border: '#CBD5E1',
  };

  const displayText = label || config.label || status;

  return (
    <span
      className={`badge rounded-pill d-inline-flex align-items-center gap-1 ${className}`}
      style={{
        color: config.color,
        backgroundColor: config.bg,
        border: `1px solid ${config.border}`,
        fontSize: '0.75rem',
        fontWeight: 500,
        padding: '0.25rem 0.65rem',
      }}
    >
      {showIcon && config.icon && <i className={`bi ${config.icon}`} style={{ fontSize: '0.75rem' }}></i>}
      <span>{displayText}</span>
    </span>
  );
}

