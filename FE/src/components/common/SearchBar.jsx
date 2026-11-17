import React, { useState } from 'react';
import Form from 'react-bootstrap/Form';
import InputGroup from 'react-bootstrap/InputGroup';

export default function SearchBar({
  value = '',
  onChange,
  onSearch,
  placeholder = 'Tìm kiếm theo từ khóa...',
  className = '',
}) {
  const [localValue, setLocalValue] = useState(value);

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      if (onSearch) onSearch(localValue);
      else if (onChange) onChange(localValue);
    }
  };

  const handleClear = () => {
    setLocalValue('');
    if (onSearch) onSearch('');
    else if (onChange) onChange('');
  };

  return (
    <InputGroup size="sm" className={`search-bar ${className}`}>
      <InputGroup.Text className="bg-white border-end-0 text-muted">
        <i className="bi bi-search"></i>
      </InputGroup.Text>
      <Form.Control
        type="text"
        placeholder={placeholder}
        value={localValue}
        onChange={(e) => {
          setLocalValue(e.target.value);
          if (onChange) onChange(e.target.value);
        }}
        onKeyDown={handleKeyDown}
        className="border-start-0 border-end-0 shadow-none"
      />
      {localValue && (
        <InputGroup.Text
          className="bg-white border-start-0 text-muted"
          style={{ cursor: 'pointer' }}
          onClick={handleClear}
          title="Xóa tìm kiếm"
        >
          <i className="bi bi-x-circle-fill"></i>
        </InputGroup.Text>
      )}
    </InputGroup>
  );
}

