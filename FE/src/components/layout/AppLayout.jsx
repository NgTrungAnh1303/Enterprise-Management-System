import React, { useState } from 'react';
import { Outlet } from 'react-router-dom';
import Offcanvas from 'react-bootstrap/Offcanvas';
import Sidebar from './Sidebar';
import Topbar from './Topbar';

export default function AppLayout() {
  const [showMobileSidebar, setShowMobileSidebar] = useState(false);

  const toggleSidebar = () => setShowMobileSidebar((prev) => !prev);
  const closeSidebar = () => setShowMobileSidebar(false);

  return (
    <div className="d-flex vh-100 overflow-hidden">
      {/* Desktop Sidebar (visible on >= 992px) */}
      <aside className="ems-sidebar d-none d-lg-flex flex-column flex-shrink-0 h-100">
        <Sidebar />
      </aside>

      {/* Mobile Offcanvas Sidebar (visible on < 992px) */}
      <Offcanvas
        show={showMobileSidebar}
        onHide={closeSidebar}
        className="ems-sidebar text-white d-lg-none"
        style={{ width: '260px', backgroundColor: 'var(--ems-bg-sidebar)' }}
      >
        <Offcanvas.Header closeButton closeVariant="white" className="border-bottom border-secondary border-opacity-25">
          <Offcanvas.Title className="fs-6 fw-bold text-white">Menu Hệ thống</Offcanvas.Title>
        </Offcanvas.Header>
        <Offcanvas.Body className="p-0">
          <Sidebar onNavigate={closeSidebar} />
        </Offcanvas.Body>
      </Offcanvas>

      {/* Main Content Area */}
      <div className="d-flex flex-column flex-grow-1 h-100 overflow-hidden">
        <Topbar onToggleSidebar={toggleSidebar} />
        <main className="flex-grow-1 p-3 p-lg-4 bg-app overflow-y-auto">
          <div className="container-fluid px-0" style={{ maxWidth: '1600px' }}>
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}

