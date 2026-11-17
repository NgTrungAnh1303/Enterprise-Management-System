import React from 'react';
import { Link } from 'react-router-dom';

export default function PageHeader({
  title,
  subtitle,
  breadcrumbs = [],
  actions,
  className = 'mb-4',
}) {
  return (
    <div className={`d-flex flex-column flex-md-row align-items-md-center justify-content-between gap-3 ${className}`}>
      <div>
        {breadcrumbs && breadcrumbs.length > 0 && (
          <nav aria-label="breadcrumb" className="mb-1">
            <ol className="breadcrumb mb-0 small" style={{ fontSize: '0.75rem' }}>
              {breadcrumbs.map((crumb, idx) => {
                const isLast = idx === breadcrumbs.length - 1;
                return (
                  <li
                    key={idx}
                    className={`breadcrumb-item ${isLast ? 'active text-secondary' : ''}`}
                    aria-current={isLast ? 'page' : undefined}
                  >
                    {!isLast && crumb.to ? (
                      <Link to={crumb.to} className="text-decoration-none text-primary">
                        {crumb.label}
                      </Link>
                    ) : (
                      crumb.label
                    )}
                  </li>
                );
              })}
            </ol>
          </nav>
        )}
        <h1 className="h4 fw-bold text-dark mb-0">{title}</h1>
        {subtitle && <p className="text-secondary small mb-0 mt-1">{subtitle}</p>}
      </div>

      {actions && <div className="d-flex align-items-center gap-2 flex-wrap">{actions}</div>}
    </div>
  );
}

