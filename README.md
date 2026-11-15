# P05 — Equipment Maintenance & Service Scheduling System (EMS)

Hệ thống quản lý bảo trì & sửa chữa thiết bị theo đúng tài liệu đặc tả `docs/SPEC_P05.md` (SRS, SecRS/IA, System Security Design).

---

## 1. Tech Stack
- **Backend:** Java 21 LTS, Spring Boot 4.0.6, Spring Security + JWT (JJWT 0.12.5), Spring Data JPA, Flyway, PostgreSQL 16.
- **Frontend:** React 18, Vite, React Router DOM, React-Bootstrap + Bootstrap 5, Formik + Yup, Chart.js.
- **Testing:** JUnit 5, Spring Boot Test, Testcontainers PostgreSQL.
- **Deployment:** Docker Compose với 3 phân vùng mạng biệt lập (`public-net`, `app-net`, `data-net`).

---

## 2. Tài khoản Demo Mặc định (Seed Dev)

> [!WARNING]
> **CẢNH BÁO MÔI TRƯỜNG PRODUCTION VỀ DỮ LIỆU SEED DEV:**  
> Dữ liệu mẫu khởi tạo trong `V3__seed_dev.sql` (bao gồm các tài khoản `ADMIN01`, `MGR01`, `TECH_A`, `USER01` với mật khẩu `Demo@123456`) **CHỈ ĐƯỢC PHÉP SỬ DỤNG CHO MỤC ĐÍCH CHẤM ĐIỂM, KIỂM THỬ VÀ DEMO**.  
> Khi triển khai môi trường Production thực tế:
> 1. Thiết lập `SPRING_PROFILES_ACTIVE=prod` (tuyệt đối không kích hoạt profile `dev`).
> 2. Đổi toàn bộ mật khẩu cơ sở dữ liệu và khóa bí mật `JWT_SECRET` trong file `.env` thành các giá trị ngẫu nhiên có độ dài $\ge 32$ bytes. Server sẽ từ chối khởi động nếu `JWT_SECRET` còn mang giá trị mẫu của repo.
> 3. Tạo tài khoản quản trị viên đầu tiên bằng script quản trị an toàn, không nạp dữ liệu seed giả lập.

Mật khẩu mặc định cho toàn bộ tài khoản demo là: **`Demo@123456`**

| Tên đăng nhập | Vai trò (Role) | Mô tả vai trò | Quyền hạn chính |
|---|---|---|---|
| `ADMIN01` | **ADMIN** | Quản trị hệ thống | Quản lý user/role, thiết bị, loại, vị trí, xem AuditLog. *Không có quyền bảo trì nghiệp vụ*. |
| `MGR01` | **MANAGER** | Quản lý bảo trì | Cấu hình kế hoạch/checklist, tạo/phân công WO, phân loại sự cố, nghiệm thu WO. |
| `MGR02` | **MANAGER** | Quản lý bảo trì | Tài khoản quản lý thứ 2 (để test nghiệm thu chéo). |
| `TECH_A` | **TECHNICIAN** | Kỹ thuật viên A | Thực hiện WO được giao (WO_A, WO_P), cập nhật checklist, nộp nghiệm thu, đề xuất OOS. |
| `TECH_B` | **TECHNICIAN** | Kỹ thuật viên B | Thực hiện WO được giao (WO_B). |
| `USER01` | **EQUIPMENT_USER** | Người dùng thiết bị | Báo sự cố (EM12), tra cứu tiến độ sự cố do chính mình báo. |
| `USER02` | **EQUIPMENT_USER** | Người dùng thiết bị | Người dùng thứ 2 (để kiểm tra phân quyền riêng tư báo cáo sự cố). |

---

## 3. Hướng dẫn chạy bằng Docker Compose

### Bước 1: Chuẩn bị biến môi trường
Tạo file `.env` từ file mẫu `.env.example`:
```bash
cp .env.example .env
```

### Bước 2: Khởi chạy các dịch vụ
```bash
# Khởi chạy Database, Backend và Frontend
docker compose up -d

# Xem log các container
docker compose logs -f
```

### Bước 3: Truy cập ứng dụng
- **Trang chủ Dashboard công khai (Khách vãng lai):** [http://localhost](http://localhost) (hoặc [http://localhost:5173](http://localhost:5173) khi chạy dev)
- **Đăng ký tài khoản tự do:** [http://localhost/register](http://localhost/register) (hoặc [http://localhost:5173/register](http://localhost:5173/register))
- **Đăng nhập hệ thống:** [http://localhost/login](http://localhost/login) (hoặc [http://localhost:5173/login](http://localhost:5173/login))
- **API Swagger UI (Backend - chỉ mở trên profile dev):** [http://localhost/swagger-ui.html](http://localhost/swagger-ui.html)
- **OpenAPI JSON Spec:** [http://localhost/v3/api-docs](http://localhost/v3/api-docs)

### Bước 4: Tài liệu & Báo cáo Nghiệm thu (Phase 6)
- **Kịch bản Demo chi tiết (QT-01, QT-02, 3 vi phạm BR):** [`docs/DEMO_SCRIPT.md`](docs/DEMO_SCRIPT.md)
- **Hướng dẫn Triển khai & Vận hành (Backup & Restore):** [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md)
- **Ma trận Truy vết Yêu cầu chức năng (EM01–EM18):** [`docs/TRACEABILITY.md`](docs/TRACEABILITY.md)
- **Báo cáo Rà soát Bảo mật Toàn diện:** [`docs/SECURITY_REVIEW.md`](docs/SECURITY_REVIEW.md)
- **Báo cáo Kết quả Kiểm thử Bảo mật & Hiệu năng:** [`docs/SECURITY_TEST_REPORT.md`](docs/SECURITY_TEST_REPORT.md)
- **Đặc tả OpenAPI 3.0 chuẩn hóa:** [`docs/openapi.yaml`](docs/openapi.yaml)
- **Quyết định Kiến trúc & Kỹ thuật:** [`docs/DECISIONS.md`](docs/DECISIONS.md)

---

## 4. Hướng dẫn chạy môi trường Phát triển trên Windows (PowerShell)

### 4.1. Chuẩn bị Cơ sở dữ liệu (PostgreSQL 16)

#### Cách 1: Sử dụng Docker Compose (Khuyên dùng nếu có Docker)
Mặc định `docker-compose.yml` không publish cổng database để tuân thủ Least Exposure. Để debug và kết nối DB từ máy host qua cổng `5433` (bind `127.0.0.1`), sử dụng kèm `docker-compose.dev.yml`:
```powershell
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d db
```

#### Cách 2: Sử dụng PostgreSQL cài đặt cục bộ trên Windows
Nếu chạy PostgreSQL trực tiếp trên máy Windows (Port `5432`):
```powershell
# Tạo cơ sở dữ liệu nếu chưa có
& "C:\Program Files\PostgreSQL\16\bin\createdb.exe" -U postgres ems_db
```

### 4.2. Danh sách Biến môi trường Cần thiết
Các biến môi trường được định nghĩa trong `.env` hoặc thiết lập trực tiếp trong PowerShell session:

| Biến môi trường | Giá trị mặc định Dev | Mô tả |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/ems_db` | Chuỗi kết nối JDBC PostgreSQL |
| `DB_HOST` | `localhost` | Địa chỉ máy chủ CSDL |
| `DB_PORT` | `5432` | Cổng CSDL |
| `DB_NAME` | `ems_db` | Tên cơ sở dữ liệu |
| `MIGRATION_USER` | `postgres` | Tài khoản chạy Flyway migration |
| `MIGRATION_PASSWORD` | `postgres` | Mật khẩu tài khoản migration |
| `APP_USER` | `app_user` | Tài khoản ứng dụng (quyền tối thiểu) |
| `APP_USER_PASSWORD` | `app_user_password_example` | Mật khẩu tài khoản app_user |
| `SERVER_PORT` | `8080` | Cổng backend Spring Boot |
| `JWT_SECRET` | `super_secret_jwt_key_must_be_at_least_256_bits_length_for_hs256!` | Khóa bí mật ký JWT (≥ 256-bit HS256) |
| `JWT_EXPIRATION_MINUTES` | `30` | Thời hạn hiệu lực của JWT (phút) |
| `APP_MAINTENANCE_DUE_SOON_DAYS` | `7` | Ngưỡng cảnh báo Đến hạn bảo trì (ngày) |
| `APP_AUDIT_MANAGER_CAN_VIEW` | `false` | Quyền xem AuditLog của Manager |
| `APP_REGISTRATION_ENABLED` | `true` | Cho phép đăng ký người dùng mới qua `POST /auth/register` (trả 404 khi false) |
| `APP_REGISTRATION_REQUIRE_APPROVAL` | `true` | Yêu cầu quản trị viên duyệt (`PENDING_APPROVAL`) trước khi tài khoản được kích hoạt |
| `APP_REGISTRATION_RATE_LIMIT_PER_HOUR` | `5` | Giới hạn tần suất đăng ký theo địa chỉ IP (lần/giờ/IP, trả 429 khi vượt) |

### 4.3. Khởi động Backend (Spring Boot Profile `dev`)
Mở cửa sổ PowerShell thứ nhất tại thư mục gốc dự án:
```powershell
cd BE

# Thiết lập các biến môi trường
$env:DB_URL = "jdbc:postgresql://localhost:5432/ems_db"
$env:MIGRATION_USER = "postgres"
$env:MIGRATION_PASSWORD = "postgres"
$env:APP_USER = "app_user"
$env:APP_USER_PASSWORD = "app_user_password_example"
$env:JWT_SECRET = "super_secret_jwt_key_must_be_at_least_256_bits_length_for_hs256!"
$env:SPRING_PROFILES_ACTIVE = "dev"

# Chạy backend qua Maven
mvn spring-boot:run
# Hoặc nếu chưa cấu hình PATH:
# & "C:\Users\nguye\.m2\wrapper\dists\apache-maven-3.9.9-bin\33b4b2b4\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run
```
Backend sẽ khởi chạy, Flyway tự động nạp `V1__init_schema.sql`, `V2__constraints_triggers.sql` và dữ liệu mẫu `V3__seed_dev.sql`, lắng nghe tại `http://localhost:8080`.

### 4.4. Khởi động Frontend (ReactJS Vite)
Mở cửa sổ PowerShell thứ hai tại thư mục gốc dự án:
```powershell
cd FE

# Cài đặt thư viện (lần đầu tiên)
npm install

# Khởi động máy chủ dev Vite
npm run dev
```
Giao diện người dùng sẽ chạy tại: **[http://localhost:5173](http://localhost:5173)**.

---

## 5. Danh sách Tài khoản Seed & Vai trò Kiểm thử

Mật khẩu chung cho toàn bộ tài khoản: **`Demo@123456`**

| Tài khoản | Vai trò | Chức năng nghiệp vụ & Kịch bản kiểm thử |
|---|---|---|
| `ADMIN01` | **ADMIN** | Quản lý người dùng/role, quản trị danh mục thiết bị/loại/vị trí, xem Audit Log. |
| `MGR01` | **MANAGER** | Tạo & phân công Work Order, cấu hình chu kỳ & checklist, phê duyệt đề xuất OOS, nghiệm thu WO. |
| `MGR02` | **MANAGER** | Quản lý thứ 2 để kiểm thử nghiệm thu chéo hoặc tạo bản ghi điều chỉnh. |
| `TECH_A` | **TECHNICIAN** | Kỹ thuật viên A: Được phân công `WO_A`, `WO_P`. Bắt đầu WO, điền checklist, nộp nghiệm thu, đề xuất OOS. |
| `TECH_B` | **TECHNICIAN** | Kỹ thuật viên B: Được phân công `WO_B`. Dùng để kiểm thử ca vi phạm BOLA/IDOR (xem/sửa WO của TECH_A bị 403). |
| `USER01` | **EQUIPMENT_USER** | Người dùng thiết bị: Tra cứu thiết bị, báo sự cố kỹ thuật, theo dõi sự cố của chính mình. |
| `USER02` | **EQUIPMENT_USER** | Người dùng thiết bị 2: Kiểm thử riêng tư (không xem được sự cố của USER01). |

---

## 5. Quy tắc Kiểm tra Bảo mật Cốt lõi (Defense-in-depth)
1. **Backend là nơi duy nhất quyết định:** Thứ tự kiểm tra: 401 (JWT) $\rightarrow$ 403 (Role) $\rightarrow$ 404 $\rightarrow$ 403 (Quyền đối tượng) $\rightarrow$ 409/422 (State machine) $\rightarrow$ 400 (DTO whitelist) $\rightarrow$ Transaction ghi.
2. **Append-Only & Bất biến tầng Database:** 4 bảng `maintenance_history`, `audit_log`, `work_order_acceptance`, `work_order_adjustment` cấm sửa/xóa cả ở quyền DB `app_user` lẫn Database Trigger.
3. **WO CLOSED bất biến:** Trigger `trg_block_closed_wo_mutation` và các trigger liên quan chặn mọi sửa đổi vào WO đã CLOSED.

