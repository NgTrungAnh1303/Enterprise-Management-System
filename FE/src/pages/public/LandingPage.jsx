import React from 'react';
import { Link } from 'react-router-dom';
import Container from 'react-bootstrap/Container';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Card from 'react-bootstrap/Card';
import Button from 'react-bootstrap/Button';
import Badge from 'react-bootstrap/Badge';
import PublicLayout from '../../components/layout/PublicLayout';

export default function LandingPage() {
  const features = [
    {
      icon: 'bi-cpu',
      color: '#1D4ED8',
      bgColor: '#EFF6FF',
      title: 'Quản lý hồ sơ thiết bị',
      desc: 'Theo dõi lý lịch chi tiết, vị trí lắp đặt, thông số kỹ thuật và trạng thái vận hành của từng trang thiết bị.',
    },
    {
      icon: 'bi-calendar-check',
      color: '#0F766E',
      bgColor: '#F0FDFA',
      title: 'Lập lịch bảo trì định kỳ và nhắc đến hạn',
      desc: 'Tự động thiết lập chu kỳ bảo dưỡng, cảnh báo trước thời hạn và ngăn ngừa sự cố ngoài ý muốn.',
    },
    {
      icon: 'bi-exclamation-octagon',
      color: '#B45309',
      bgColor: '#FFFBEB',
      title: 'Tiếp nhận và xử lý sự cố',
      desc: 'Ghi nhận yêu cầu hỏng hóc từ người dùng, đánh giá mức độ nghiêm trọng và điều phối kỹ thuật viên tức thời.',
    },
    {
      icon: 'bi-journal-check',
      color: '#4338CA',
      bgColor: '#EEF2FF',
      title: 'Lưu lịch sử bảo trì, truy vết đầy đủ',
      desc: 'Ghi nhật ký bảo trì bất biến, lưu trữ kết quả kiểm tra và báo cáo nghiệm thu phục vụ kiểm toán.',
    },
  ];

  const workflowSteps = [
    {
      step: '1',
      title: 'Báo sự cố',
      icon: 'bi-megaphone',
      desc: 'Người dùng phát hiện hỏng hóc và gửi yêu cầu hỗ trợ kèm mô tả chi tiết.',
    },
    {
      step: '2',
      title: 'Phân công kỹ thuật viên',
      icon: 'bi-person-gear',
      desc: 'Quản lý đánh giá mức độ nghiêm trọng và giao việc cho kỹ thuật viên phù hợp.',
    },
    {
      step: '3',
      title: 'Thực hiện bảo trì',
      icon: 'bi-wrench-adjustable',
      desc: 'Kỹ thuật viên kiểm tra thực tế, cập nhật checklist và ghi nhận kết quả sửa chữa.',
    },
    {
      step: '4',
      title: 'Nghiệm thu và đóng phiếu',
      icon: 'bi-patch-check-fill',
      desc: 'Quản lý kiểm tra chất lượng độc lập, ký duyệt nghiệm thu và hoàn tất hồ sơ.',
    },
  ];

  const roles = [
    {
      role: 'Người dùng thiết bị',
      badgeText: 'EQUIPMENT_USER',
      badgeBg: 'primary',
      icon: 'bi-person',
      desc: 'Gửi yêu cầu báo hỏng nhanh chóng và chủ động theo dõi tiến độ xử lý thiết bị của phòng ban.',
    },
    {
      role: 'Kỹ thuật viên',
      badgeText: 'TECHNICIAN',
      badgeBg: 'info',
      icon: 'bi-tools',
      desc: 'Tiếp nhận phiếu công tác, thực hiện kiểm tra kỹ thuật theo checklist chuẩn hóa và đề xuất giải pháp.',
    },
    {
      role: 'Quản lý',
      badgeText: 'MANAGER',
      badgeBg: 'warning',
      badgeTextColor: 'dark',
      icon: 'bi-clipboard-data',
      desc: 'Điều phối nguồn lực bảo trì, phân loại mức độ khẩn cấp, nghiệm thu độc lập và giám sát an toàn vận hành.',
    },
    {
      role: 'Quản trị viên',
      badgeText: 'ADMIN',
      badgeBg: 'danger',
      icon: 'bi-shield-lock',
      desc: 'Khởi tạo và phê duyệt tài khoản người dùng, phân bổ quyền hạn và quản lý toàn vẹn nhật ký hệ thống.',
    },
  ];

  return (
    <PublicLayout>
      <div className="landing-page">
        {}
        <section className="py-4 py-lg-5 text-center" aria-labelledby="hero-heading">
          <Container className="px-3" style={{ maxWidth: '860px' }}>
            <div
              className="d-inline-flex align-items-center justify-content-center rounded-circle mb-3 text-white shadow-sm"
              style={{ width: '64px', height: '64px', backgroundColor: '#1D4ED8' }}
              aria-hidden="true"
            >
              <i className="bi bi-shield-shaded fs-2"></i>
            </div>
            <h1 id="hero-heading" className="display-6 fw-bold text-dark mb-3">
              Hệ thống quản lý bảo trì và lịch bảo dưỡng thiết bị
            </h1>
            <p className="lead text-secondary mb-4 mx-auto" style={{ maxWidth: '720px', fontSize: '1.15rem' }}>
              Giải pháp toàn diện về quản trị hồ sơ thiết bị, tự động điều phối lịch bảo dưỡng định kỳ và kiểm soát quy trình xử lý sự cố kỹ thuật theo thời gian thực.
            </p>
            <div className="d-flex flex-wrap justify-content-center gap-3">
              <Button
                as={Link}
                to="/login"
                variant="primary"
                size="lg"
                className="px-4 py-2 fw-semibold d-inline-flex align-items-center gap-2 shadow-sm"
                aria-label="Đăng nhập vào hệ thống"
              >
                <i className="bi bi-box-arrow-in-right"></i>
                <span>Đăng nhập</span>
              </Button>
              <Button
                as={Link}
                to="/register"
                variant="outline-primary"
                size="lg"
                className="px-4 py-2 fw-semibold d-inline-flex align-items-center gap-2"
                aria-label="Đăng ký tài khoản mới"
              >
                <i className="bi bi-person-plus"></i>
                <span>Đăng ký tài khoản</span>
              </Button>
            </div>
          </Container>
        </section>

        {}
        <section className="py-5" aria-labelledby="features-heading">
          <div className="text-center mb-4">
            <h2 id="features-heading" className="h4 fw-bold text-dark mb-2">
              Hệ thống làm được gì
            </h2>
            <p className="text-secondary small mb-0">
              Bốn năng lực cốt lõi giúp chuẩn hóa và tối ưu quy trình bảo trì trang thiết bị
            </p>
          </div>
          <Row className="g-4">
            {features.map((feat, idx) => (
              <Col xs={12} sm={6} lg={3} key={idx}>
                <Card className="h-100 border border-light-subtle shadow-sm">
                  <Card.Body className="p-4 d-flex flex-column">
                    <div
                      className="d-inline-flex align-items-center justify-content-center rounded-3 mb-3"
                      style={{
                        width: '48px',
                        height: '48px',
                        backgroundColor: feat.bgColor,
                        color: feat.color,
                      }}
                      aria-hidden="true"
                    >
                      <i className={`bi ${feat.icon} fs-4`}></i>
                    </div>
                    <h3 className="h6 fw-bold text-dark mb-2">{feat.title}</h3>
                    <p className="text-secondary small mb-0 flex-grow-1 lh-base">{feat.desc}</p>
                  </Card.Body>
                </Card>
              </Col>
            ))}
          </Row>
        </section>

        {}
        <section className="py-5" aria-labelledby="workflow-heading">
          <div className="text-center mb-4">
            <h2 id="workflow-heading" className="h4 fw-bold text-dark mb-2">
              Quy trình 4 bước khép kín
            </h2>
            <p className="text-secondary small mb-0">
              Quy trình vận hành phối hợp nhịp nhàng giữa người sử dụng, kỹ thuật viên và cấp quản lý
            </p>
          </div>
          <Card className="border border-light-subtle shadow-sm p-3 p-md-4">
            <Row className="g-4 position-relative">
              {workflowSteps.map((step, idx) => (
                <Col xs={12} sm={6} lg={3} key={idx}>
                  <div className="d-flex flex-column h-100">
                    <div className="d-flex align-items-center gap-2 mb-2">
                      <div
                        className="d-flex align-items-center justify-content-center rounded-circle bg-primary text-white fw-bold small flex-shrink-0"
                        style={{ width: '28px', height: '28px' }}
                      >
                        {step.step}
                      </div>
                      <span className="fw-bold text-dark small text-uppercase tracking-wide">
                        Bước {step.step}
                      </span>
                    </div>
                    <div className="d-flex align-items-center gap-2 mb-2 text-primary">
                      <i className={`bi ${step.icon} fs-5`}></i>
                      <h3 className="h6 fw-bold text-dark mb-0">{step.title}</h3>
                    </div>
                    <p className="text-secondary small mb-0 lh-base">{step.desc}</p>
                  </div>
                </Col>
              ))}
            </Row>
          </Card>
        </section>

        {}
        <section className="py-5" aria-labelledby="roles-heading">
          <div className="text-center mb-4">
            <h2 id="roles-heading" className="h4 fw-bold text-dark mb-2">
              Bốn vai trò trong hệ thống
            </h2>
            <p className="text-secondary small mb-0">
              Phân định rõ trách nhiệm của từng cá nhân theo mô hình kiểm soát nội bộ
            </p>
          </div>
          <Row className="g-4">
            {roles.map((item, idx) => (
              <Col xs={12} sm={6} lg={3} key={idx}>
                <Card className="h-100 border border-light-subtle shadow-sm">
                  <Card.Body className="p-4 d-flex flex-column">
                    <div className="d-flex align-items-center justify-content-between mb-3">
                      <div
                        className="d-inline-flex align-items-center justify-content-center rounded text-white"
                        style={{ width: '38px', height: '38px', backgroundColor: '#334155' }}
                        aria-hidden="true"
                      >
                        <i className={`bi ${item.icon} fs-5`}></i>
                      </div>
                      <Badge bg={item.badgeBg} text={item.badgeTextColor} style={{ fontSize: '0.7rem' }}>
                        {item.badgeText}
                      </Badge>
                    </div>
                    <h3 className="h6 fw-bold text-dark mb-2">{item.role}</h3>
                    <p className="text-secondary small mb-0 flex-grow-1 lh-base">{item.desc}</p>
                  </Card.Body>
                </Card>
              </Col>
            ))}
          </Row>
        </section>

        {}
        <section className="py-4 my-3 text-center" aria-labelledby="cta-heading">
          <Card className="border border-light-subtle shadow-sm bg-white p-4 p-md-5">
            <Container style={{ maxWidth: '640px' }}>
              <h2 id="cta-heading" className="h4 fw-bold text-dark mb-2">
                Bắt đầu làm việc với hệ thống
              </h2>
              <p className="text-secondary small mb-4">
                Đăng nhập ngay để tiếp nhận công việc, quản lý lịch bảo dưỡng hoặc gửi yêu cầu xử lý sự cố thiết bị.
              </p>
              <div className="d-flex flex-wrap justify-content-center align-items-center gap-3">
                <Button
                  as={Link}
                  to="/login"
                  variant="primary"
                  className="px-4 py-2 fw-semibold d-inline-flex align-items-center gap-2"
                >
                  <i className="bi bi-box-arrow-in-right"></i>
                  <span>Đăng nhập để vào hệ thống</span>
                </Button>
                <div className="text-secondary small">
                  <span>Chưa có tài khoản? </span>
                  <Link to="/register" className="text-primary fw-semibold text-decoration-none">
                    Đăng ký
                  </Link>
                </div>
              </div>
            </Container>
          </Card>
        </section>
      </div>
    </PublicLayout>
  );
}
