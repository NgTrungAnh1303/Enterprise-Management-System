import React from 'react';
import Card from 'react-bootstrap/Card';
import Badge from 'react-bootstrap/Badge';
import PageHeader from '../../components/common/PageHeader';

export default function PlaceholderPage({
  title = 'Chức năng đang phát triển',
  subtitle = 'Phân hệ chức năng theo đặc tả SPEC P05',
  scrId = 'SCR',
}) {
  return (
    <div>
      <PageHeader
        title={title}
        subtitle={subtitle}
        breadcrumbs={[
          { label: 'Trang chủ', to: '/' },
          { label: scrId },
        ]}
      />

      <Card className="border border-light-subtle shadow-sm text-center p-5">
        <Card.Body className="py-5">
          <Badge bg="primary" className="mb-3 px-3 py-2 fs-6">
            {scrId}
          </Badge>
          <h2 className="h5 fw-bold text-dark mb-2">{title}</h2>
          <p className="text-secondary small mb-4" style={{ maxWidth: '480px', margin: '0 auto' }}>
            Màn hình này đã được cấu hình đường dẫn và phân quyền theo thiết kế. Nội dung chi tiết sẽ được hoàn thiện trong Phase 5 & Phase 6.
          </p>
          <div className="d-inline-flex align-items-center gap-2 px-3 py-2 rounded bg-light border text-muted small">
            <i className="bi bi-clock-history"></i>
            <span>Trạng thái: Nền tảng điều hướng hoàn tất (Phase 4 Foundation)</span>
          </div>
        </Card.Body>
      </Card>
    </div>
  );
}

