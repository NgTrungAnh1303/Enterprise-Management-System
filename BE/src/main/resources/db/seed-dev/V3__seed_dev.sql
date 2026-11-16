-- V3__seed_dev.sql
-- Seed data for Development and Demonstration environments
-- BCrypt hash for default demo password "Demo@123456":
-- $2a$10$7EqJtq98hPqEX7fNZaFWoOhi5F7jB3hD8Dq3bUeN6N3q2M7N6q7Gy

-- 1. Seed Roles
INSERT INTO role (role_id, name) VALUES
(1, 'ADMIN'),
(2, 'MANAGER'),
(3, 'TECHNICIAN'),
(4, 'EQUIPMENT_USER')
ON CONFLICT (role_id) DO NOTHING;

-- 2. Seed Users (BCrypt hash for "Demo@123456")
INSERT INTO app_user (user_id, username, password_hash, full_name, status, role_id, failed_login_count, locked_until) VALUES
(1, 'ADMIN01', '$2a$10$v.k8kFN0e83C2siSh96pKOuOXJjqlxiBvNbJtXK3aJhqs52k0JXUG', 'System Administrator', 'ACTIVE', 1, 0, NULL),
(2, 'MGR01',   '$2a$10$v.k8kFN0e83C2siSh96pKOuOXJjqlxiBvNbJtXK3aJhqs52k0JXUG', 'Nguyen Van Manager',    'ACTIVE', 2, 0, NULL),
(3, 'MGR02',   '$2a$10$v.k8kFN0e83C2siSh96pKOuOXJjqlxiBvNbJtXK3aJhqs52k0JXUG', 'Tran Thi Manager',      'ACTIVE', 2, 0, NULL),
(4, 'TECH_A',  '$2a$10$v.k8kFN0e83C2siSh96pKOuOXJjqlxiBvNbJtXK3aJhqs52k0JXUG', 'Le Van Technician A',   'ACTIVE', 3, 0, NULL),
(5, 'TECH_B',  '$2a$10$v.k8kFN0e83C2siSh96pKOuOXJjqlxiBvNbJtXK3aJhqs52k0JXUG', 'Pham Van Technician B', 'ACTIVE', 3, 0, NULL),
(6, 'USER01',  '$2a$10$v.k8kFN0e83C2siSh96pKOuOXJjqlxiBvNbJtXK3aJhqs52k0JXUG', 'Hoang User One',        'ACTIVE', 4, 0, NULL),
(7, 'USER02',  '$2a$10$v.k8kFN0e83C2siSh96pKOuOXJjqlxiBvNbJtXK3aJhqs52k0JXUG', 'Vu User Two',           'ACTIVE', 4, 0, NULL)
ON CONFLICT (user_id) DO NOTHING;

-- 3. Seed Locations
INSERT INTO location (location_id, room, area, department) VALUES
(1, 'P.101', 'Khu A', 'Phòng Thí Nghiệm Cơ Điện Tử'),
(2, 'P.202', 'Khu B', 'Phòng Thí Nghiệm Hóa Sinh'),
(3, 'P.303', 'Khu C', 'Trung Tâm Mạng & Máy Tính')
ON CONFLICT (location_id) DO NOTHING;

-- 4. Seed Equipment Types
INSERT INTO equipment_type (type_id, name, description) VALUES
(1, 'Máy Ly Tâm', 'Máy ly tâm phục vụ phân tách mẫu hóa sinh trong lab'),
(2, 'Kính Hiển Vi Quang Học', 'Kính hiển vi độ phóng đại cao cho nghiên cứu vi sinh'),
(3, 'Máy Phân Tích Quang Phổ', 'Thiết bị đo độ hấp thụ quang và phân tích mẫu vật')
ON CONFLICT (type_id) DO NOTHING;

-- 5. Seed Equipment (EQ01 - EQ10)
INSERT INTO equipment (equipment_id, code, brand, model, serial, status, type_id, location_id) VALUES
(1,  'EQ01', 'Eppendorf', '5810R',    'SN-EP-5810-01', 'ACTIVE', 1, 1),
(2,  'EQ02', 'Thermo',    'SL16R',    'SN-TH-16R-02',  'ACTIVE', 1, 1),
(3,  'EQ03', 'Olympus',   'CX23',     'SN-OL-CX23-03', 'ACTIVE', 2, 2),
(4,  'EQ04', 'Nikon',     'Eclipse',  'SN-NK-ECL-04',  'ACTIVE', 2, 2),
(5,  'EQ05', 'Shimadzu',  'UV-1900i', 'SN-SH-1900-05', 'ACTIVE', 3, 3),
(6,  'EQ06', 'Agilent',   'Cary 60',  'SN-AG-C60-06',  'ACTIVE', 3, 3),
(7,  'EQ07', 'Eppendorf', 'MiniSpin', 'SN-EP-MS-07',   'ACTIVE', 1, 1),
(8,  'EQ08', 'Olympus',   'BX43',     'SN-OL-BX43-08', 'ACTIVE', 2, 2),
(9,  'EQ09', 'Perkin',    'Lambda 365','SN-PK-L36-09', 'ACTIVE', 3, 3),
(10, 'EQ10', 'Thermo',    'BioMate',  'SN-TH-BM-10',   'OUT_OF_SERVICE', 3, 3)
ON CONFLICT (equipment_id) DO NOTHING;

-- 6. Seed Maintenance Plans (cycle_days 30, 90, 180)
INSERT INTO maintenance_plan (plan_id, equipment_id, cycle_days, last_completed_date, next_due_date) VALUES
(1,  1,  90,  CURRENT_DATE - INTERVAL '100 days', CURRENT_DATE - INTERVAL '10 days'), -- Overdue
(2,  2,  30,  CURRENT_DATE - INTERVAL '27 days',  CURRENT_DATE + INTERVAL '3 days'),  -- Due Soon
(3,  3,  90,  CURRENT_DATE - INTERVAL '30 days',  CURRENT_DATE + INTERVAL '60 days'), -- Far
(4,  4,  180, CURRENT_DATE - INTERVAL '180 days', CURRENT_DATE - INTERVAL '1 day'),   -- Overdue
(5,  5,  90,  CURRENT_DATE - INTERVAL '85 days',  CURRENT_DATE + INTERVAL '5 days'),  -- Due Soon
(6,  6,  30,  CURRENT_DATE - INTERVAL '10 days',  CURRENT_DATE + INTERVAL '20 days'), -- Far
(7,  7,  90,  CURRENT_DATE - INTERVAL '20 days',  CURRENT_DATE + INTERVAL '70 days'),
(8,  8,  180, CURRENT_DATE - INTERVAL '50 days',  CURRENT_DATE + INTERVAL '130 days'),
(9,  9,  90,  CURRENT_DATE - INTERVAL '88 days',  CURRENT_DATE + INTERVAL '2 days'),  -- Due Soon
(10, 10, 90,  CURRENT_DATE - INTERVAL '95 days',  CURRENT_DATE - INTERVAL '5 days')
ON CONFLICT (plan_id) DO NOTHING;

-- 7. Seed Checklists
INSERT INTO maintenance_checklist (checklist_id, type_id, item, is_required) VALUES
(1, 1, 'Kiểm tra độ cân bằng rotor và độ rơ trục quay', TRUE),
(2, 1, 'Vệ sinh khoang ly tâm và bôi trơn đệm cao su nắp', TRUE),
(3, 1, 'Kiểm tra tốc độ quay bằng máy đo quang học', FALSE),
(4, 2, 'Lau sạch thấu kính và thị kính bằng cồn chuyên dụng', TRUE),
(5, 2, 'Kiểm tra độ hội tụ và nguồn sáng LED/Halogen', TRUE),
(6, 2, 'Hiệu chuẩn thước đo vi trường', FALSE),
(7, 3, 'Kiểm tra nguồn đèn Deuterium / Tungsten', TRUE),
(8, 3, 'Hiệu chuẩn bước sóng bằng kính lọc chuẩn Holmium', TRUE),
(9, 3, 'Vệ sinh buồng đo mẫu cuvet', TRUE)
ON CONFLICT (checklist_id) DO NOTHING;

-- 8. Seed Work Orders (WO_A, WO_B, WO_P, WO_C - KHÔNG seed WO_V)
-- Lưu ý: WO_C được khởi tạo ở SUBMITTED để cho phép chèn phân công & kết quả kiểm tra trước khi chuyển sang CLOSED
INSERT INTO maintenance_work_order (wo_id, wo_code, equipment_id, plan_id, incident_id, type, period, status, created_at, closed_at) VALUES
(1, 'WO_A', 1, 1, NULL, 'PERIODIC', '2026-Q1', 'ASSIGNED',  CURRENT_TIMESTAMP - INTERVAL '3 days', NULL),
(2, 'WO_B', 2, 2, NULL, 'PERIODIC', '2026-Q1', 'ASSIGNED',  CURRENT_TIMESTAMP - INTERVAL '2 days', NULL),
(3, 'WO_P', 3, 3, NULL, 'PERIODIC', '2026-Q1', 'SUBMITTED', CURRENT_TIMESTAMP - INTERVAL '5 days', NULL),
(4, 'WO_C', 4, 4, NULL, 'PERIODIC', '2025-Q4', 'SUBMITTED', CURRENT_TIMESTAMP - INTERVAL '90 days', NULL)
ON CONFLICT (wo_id) DO NOTHING;

-- 9. Seed Work Order Assignments
INSERT INTO work_order_assignment (assignment_id, wo_id, technician_id, assigned_by, assigned_at, is_current, unassigned_at) VALUES
(1, 1, 4, 2, CURRENT_TIMESTAMP - INTERVAL '3 days', TRUE, NULL), -- WO_A assigned to TECH_A by MGR01
(2, 2, 5, 2, CURRENT_TIMESTAMP - INTERVAL '2 days', TRUE, NULL), -- WO_B assigned to TECH_B by MGR01
(3, 3, 4, 2, CURRENT_TIMESTAMP - INTERVAL '5 days', TRUE, NULL), -- WO_P assigned to TECH_A by MGR01
(4, 4, 4, 2, CURRENT_TIMESTAMP - INTERVAL '90 days', TRUE, NULL) -- WO_C assigned to TECH_A by MGR01
ON CONFLICT (assignment_id) DO NOTHING;

-- 10. Seed Inspection Results for WO_P (completed) and WO_C (closed)
INSERT INTO inspection_result (result_id, wo_id, checklist_id, done, condition, defect, work_done, entered_by, entered_at) VALUES
(1, 3, 4, TRUE, 'Thấu kính bám bụi nhẹ', 'Bụi mờ vi trường', 'Đã lau sạch bằng dung dịch cồn quang học', 4, CURRENT_TIMESTAMP - INTERVAL '1 day'),
(2, 3, 5, TRUE, 'Nguồn sáng LED sáng tốt', 'Không có lỗi', 'Kiểm tra độ chói đạt chuẩn', 4, CURRENT_TIMESTAMP - INTERVAL '1 day'),
(3, 3, 6, TRUE, 'Thước đo vi trường chính xác', 'Không có lỗi', 'Đã đối chiếu tiêu bản chuẩn', 4, CURRENT_TIMESTAMP - INTERVAL '1 day'),
(4, 4, 4, TRUE, 'Bình thường', 'Không phát hiện lỗi', 'Đã vệ sinh định kỳ', 4, CURRENT_TIMESTAMP - INTERVAL '80 days'),
(5, 4, 5, TRUE, 'Nguồn sáng tốt', 'Không có lỗi', 'Đã kiểm tra', 4, CURRENT_TIMESTAMP - INTERVAL '80 days')
ON CONFLICT (result_id) DO NOTHING;

-- 11. Seed Acceptance for WO_C
INSERT INTO work_order_acceptance (acceptance_id, wo_id, decision, note, accepted_by, accepted_at) VALUES
(1, 4, 'PASS', 'Đạt yêu cầu kiểm tra định kỳ Q4/2025', 2, CURRENT_TIMESTAMP - INTERVAL '80 days')
ON CONFLICT (acceptance_id) DO NOTHING;

-- Chuyển trạng thái WO_C từ SUBMITTED sang CLOSED sau khi đã hoàn tất nghiệm thu PASS
UPDATE maintenance_work_order 
SET status = 'CLOSED', closed_at = CURRENT_TIMESTAMP - INTERVAL '80 days'
WHERE wo_id = 4 AND status <> 'CLOSED';

-- 12. Seed Incident Reports
INSERT INTO incident_report (incident_id, tracking_code, equipment_id, reporter_id, description, severity, status, created_at) VALUES
(1, 'INC-2026-0001', 5, 6, 'Máy phân tích phát ra tiếng ồn bất thường khi khởi động', 'HIGH', 'REPORTED', CURRENT_TIMESTAMP - INTERVAL '1 day'),
(2, 'INC-2026-0002', 6, 7, 'Màn hình hiển thị chập chờn, kết quả đo không ổn định', 'CRITICAL', 'CLASSIFIED', CURRENT_TIMESTAMP - INTERVAL '2 days')
ON CONFLICT (incident_id) DO NOTHING;

-- Update sequences to avoid primary key conflict on subsequent inserts
SELECT setval('role_role_id_seq', (SELECT MAX(role_id) FROM role));
SELECT setval('app_user_user_id_seq', (SELECT MAX(user_id) FROM app_user));
SELECT setval('location_location_id_seq', (SELECT MAX(location_id) FROM location));
SELECT setval('equipment_type_type_id_seq', (SELECT MAX(type_id) FROM equipment_type));
SELECT setval('equipment_equipment_id_seq', (SELECT MAX(equipment_id) FROM equipment));
SELECT setval('maintenance_plan_plan_id_seq', (SELECT MAX(plan_id) FROM maintenance_plan));
SELECT setval('maintenance_checklist_checklist_id_seq', (SELECT MAX(checklist_id) FROM maintenance_checklist));
SELECT setval('maintenance_work_order_wo_id_seq', (SELECT MAX(wo_id) FROM maintenance_work_order));
SELECT setval('work_order_assignment_assignment_id_seq', (SELECT MAX(assignment_id) FROM work_order_assignment));
SELECT setval('inspection_result_result_id_seq', (SELECT MAX(result_id) FROM inspection_result));
SELECT setval('work_order_acceptance_acceptance_id_seq', (SELECT MAX(acceptance_id) FROM work_order_acceptance));
SELECT setval('incident_report_incident_id_seq', (SELECT MAX(incident_id) FROM incident_report));
