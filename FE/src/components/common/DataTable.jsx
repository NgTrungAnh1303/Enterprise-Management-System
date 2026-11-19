import React from 'react';
import Table from 'react-bootstrap/Table';
import LoadingState from './LoadingState';
import EmptyState from './EmptyState';
import ErrorState from './ErrorState';
import Pagination from './Pagination';

export default function DataTable({
  columns = [],
  data = [],
  loading = false,
  error = null,
  emptyTitle = 'Không có dữ liệu',
  emptyMessage = 'Không tìm thấy bản ghi nào phù hợp với bộ lọc hiện tại.',
  onRetry,
  pagination,
  keyField = 'id',
  className = '',
}) {
  if (loading) {
    return (
      <div className={`ems-table-card p-4 ${className}`}>
        <LoadingState message="Đang nạp dữ liệu bảng..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className={`ems-table-card p-4 ${className}`}>
        <ErrorState
          title="Không thể tải danh sách dữ liệu"
          message={error.message || 'Đã có sự cố kết nối máy chủ.'}
          code={error.code}
          requestId={error.requestId}
          onRetry={onRetry}
        />
      </div>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div className={`ems-table-card ${className}`}>
        <EmptyState title={emptyTitle} message={emptyMessage} />
      </div>
    );
  }

  return (
    <div className={`ems-table-card ${className}`}>
      <div className="table-responsive mb-0">
        <Table hover className="ems-table align-middle mb-0">
          <thead>
            <tr>
              {columns.map((col, idx) => (
                <th
                  key={col.key || col.field || idx}
                  className={col.headerClassName || ''}
                  style={col.width ? { width: col.width } : undefined}
                >
                  {col.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.map((row, rowIdx) => {
              const rowKey = row[keyField] != null ? row[keyField] : rowIdx;
              return (
                <tr key={rowKey}>
                  {columns.map((col, colIdx) => {
                    const cellKey = col.key || col.field;
                    const rawVal = cellKey != null ? row[cellKey] : undefined;
                    let cellContent = '—';
                    if (col.render) {
                      cellContent = col.render(row, rowIdx);
                    } else if (rawVal != null) {
                      cellContent = typeof rawVal === 'object' ? JSON.stringify(rawVal) : String(rawVal);
                    }

                    // Protect against any rendered objects that are not valid React elements
                    if (cellContent != null && typeof cellContent === 'object' && !React.isValidElement(cellContent)) {
                      cellContent = String(cellContent.label || cellContent.name || cellContent.title || cellContent.code || JSON.stringify(cellContent));
                    }

                    return (
                      <td key={cellKey || colIdx} className={col.cellClassName || ''}>
                        {cellContent}
                      </td>
                    );
                  })}
                </tr>
              );
            })}
          </tbody>
        </Table>
      </div>

      {pagination && (
        <Pagination
          page={pagination.page}
          size={pagination.size}
          total={pagination.total}
          onPageChange={pagination.onPageChange}
          onSizeChange={pagination.onSizeChange}
          sizeOptions={pagination.sizeOptions}
        />
      )}
    </div>
  );
}

