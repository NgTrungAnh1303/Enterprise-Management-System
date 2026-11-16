-- V5__seed_roles_and_master_data.sql
-- Seed initial master roles, users, and baseline domain data for EMS

-- 1. Core Roles
INSERT INTO role (role_id, name) VALUES
(1, 'ADMIN'),
(2, 'MANAGER'),
(3, 'TECHNICIAN'),
(4, 'EQUIPMENT_USER')
ON CONFLICT (role_id) DO UPDATE SET name = EXCLUDED.name;

SELECT setval('role_role_id_seq', (SELECT GREATEST(MAX(role_id), 4) FROM role));

-- 2. Initial Users (BCrypt hash for "Demo@123456": $2a$10$v.k8kFN0e83C2siSh96pKOuOXJjqlxiBvNbJtXK3aJhqs52k0JXUG)
INSERT INTO app_user (user_id, username, email, password_hash, full_name, status, role_id, failed_login_count, locked_until) VALUES
(1, 'ADMIN01', 'admin01@ems.fpt.edu.vn', '$2a$10$v.k8kFN0e83C2siSh96pKOuOXJjqlxiBvNbJtXK3aJhqs52k0JXUG', 'System Administrator', 'ACTIVE', 1, 0, NULL),
(2, 'MGR01',   'mgr01@ems.fpt.edu.vn',   '$2a$10$v.k8kFN0e83C2siSh96pKOuOXJjqlxiBvNbJtXK3aJhqs52k0JXUG', 'Nguyen Van Manager',    'ACTIVE', 2, 0, NULL),
(3, 'MGR02',   'mgr02@ems.fpt.edu.vn',   '$2a$10$v.k8kFN0e83C2siSh96pKOuOXJjqlxiBvNbJtXK3aJhqs52k0JXUG', 'Tran Thi Manager',      'ACTIVE', 2, 0, NULL),
(4, 'TECH_A',  'tech_a@ems.fpt.edu.vn',  '$2a$10$v.k8kFN0e83C2siSh96pKOuOXJjqlxiBvNbJtXK3aJhqs52k0JXUG', 'Le Van Technician A',   'ACTIVE', 3, 0, NULL),
(5, 'TECH_B',  'tech_b@ems.fpt.edu.vn',  '$2a$10$v.k8kFN0e83C2siSh96pKOuOXJjqlxiBvNbJtXK3aJhqs52k0JXUG', 'Pham Van Technician B', 'ACTIVE', 3, 0, NULL),
(6, 'USER01',  'user01@ems.fpt.edu.vn',  '$2a$10$v.k8kFN0e83C2siSh96pKOuOXJjqlxiBvNbJtXK3aJhqs52k0JXUG', 'Hoang User One',        'ACTIVE', 4, 0, NULL),
(7, 'USER02',  'user02@ems.fpt.edu.vn',  '$2a$10$v.k8kFN0e83C2siSh96pKOuOXJjqlxiBvNbJtXK3aJhqs52k0JXUG', 'Vu User Two',           'ACTIVE', 4, 0, NULL)
ON CONFLICT (user_id) DO NOTHING;

SELECT setval('app_user_user_id_seq', (SELECT GREATEST(MAX(user_id), 7) FROM app_user));

-- 3. Locations
INSERT INTO location (location_id, room, area, department) VALUES
(1, 'P.101', 'Khu A', 'Phòng Thí Nghiệm Cơ Điện Tử'),
(2, 'P.202', 'Khu B', 'Phòng Thí Nghiệm Hóa Sinh'),
(3, 'P.303', 'Khu C', 'Trung Tâm Mạng & Máy Tính')
ON CONFLICT (location_id) DO NOTHING;

SELECT setval('location_location_id_seq', (SELECT GREATEST(MAX(location_id), 3) FROM location));

-- 4. Equipment Types
INSERT INTO equipment_type (type_id, name, description) VALUES
(1, 'Máy Ly Tâm', 'Máy ly tâm phục vụ phân tách mẫu hóa sinh trong lab'),
(2, 'Kính Hiển Vi Quang Học', 'Kính hiển vi độ phóng đại cao cho nghiên cứu vi sinh'),
(3, 'Máy Phân Tích Quang Phổ', 'Thiết bị đo độ hấp thụ quang và phân tích mẫu vật')
ON CONFLICT (type_id) DO NOTHING;

SELECT setval('equipment_type_type_id_seq', (SELECT GREATEST(MAX(type_id), 3) FROM equipment_type));

-- 5. Equipment (EQ01 - EQ10)
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

SELECT setval('equipment_equipment_id_seq', (SELECT GREATEST(MAX(equipment_id), 10) FROM equipment));

-- 6. Maintenance Plans
INSERT INTO maintenance_plan (plan_id, equipment_id, cycle_days, last_completed_date, next_due_date) VALUES
(1,  1,  90,  CURRENT_DATE - INTERVAL '100 days', CURRENT_DATE - INTERVAL '10 days'),
(2,  2,  30,  CURRENT_DATE - INTERVAL '27 days',  CURRENT_DATE + INTERVAL '3 days'),
(3,  3,  90,  CURRENT_DATE - INTERVAL '30 days',  CURRENT_DATE + INTERVAL '60 days'),
(4,  4,  180, CURRENT_DATE - INTERVAL '180 days', CURRENT_DATE - INTERVAL '1 day'),
(5,  5,  90,  CURRENT_DATE - INTERVAL '85 days',  CURRENT_DATE + INTERVAL '5 days'),
(6,  6,  30,  CURRENT_DATE - INTERVAL '10 days',  CURRENT_DATE + INTERVAL '20 days'),
(7,  7,  90,  CURRENT_DATE - INTERVAL '20 days',  CURRENT_DATE + INTERVAL '70 days'),
(8,  8,  180, CURRENT_DATE - INTERVAL '50 days',  CURRENT_DATE + INTERVAL '130 days'),
(9,  9,  90,  CURRENT_DATE - INTERVAL '88 days',  CURRENT_DATE + INTERVAL '2 days'),
(10, 10, 90,  CURRENT_DATE - INTERVAL '95 days',  CURRENT_DATE - INTERVAL '5 days')
ON CONFLICT (plan_id) DO NOTHING;

SELECT setval('maintenance_plan_plan_id_seq', (SELECT GREATEST(MAX(plan_id), 10) FROM maintenance_plan));

-- 7. Maintenance Checklists
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

SELECT setval('maintenance_checklist_checklist_id_seq', (SELECT GREATEST(MAX(checklist_id), 9) FROM maintenance_checklist));

-- 8. Work Orders
INSERT INTO maintenance_work_order (wo_id, wo_code, equipment_id, plan_id, incident_id, type, period, status, created_at, closed_at) VALUES
(1, 'WO_A', 1, 1, NULL, 'PERIODIC', '2026-Q1', 'ASSIGNED',  CURRENT_TIMESTAMP - INTERVAL '3 days', NULL),
(2, 'WO_B', 2, 2, NULL, 'PERIODIC', '2026-Q1', 'ASSIGNED',  CURRENT_TIMESTAMP - INTERVAL '2 days', NULL),
(3, 'WO_P', 3, 3, NULL, 'PERIODIC', '2026-Q1', 'SUBMITTED', CURRENT_TIMESTAMP - INTERVAL '5 days', NULL),
(4, 'WO_C', 4, 4, NULL, 'PERIODIC', '2025-Q4', 'SUBMITTED', CURRENT_TIMESTAMP - INTERVAL '90 days', NULL)
ON CONFLICT (wo_id) DO NOTHING;

SELECT setval('maintenance_work_order_wo_id_seq', (SELECT GREATEST(MAX(wo_id), 4) FROM maintenance_work_order));

-- 9. Work Order Assignments
INSERT INTO work_order_assignment (assignment_id, wo_id, technician_id, assigned_by, assigned_at, is_current, unassigned_at) VALUES
(1, 1, 4, 2, CURRENT_TIMESTAMP - INTERVAL '3 days', TRUE, NULL),
(2, 2, 5, 2, CURRENT_TIMESTAMP - INTERVAL '2 days', TRUE, NULL),
(3, 3, 4, 2, CURRENT_TIMESTAMP - INTERVAL '5 days', TRUE, NULL),
(4, 4, 4, 2, CURRENT_TIMESTAMP - INTERVAL '90 days', TRUE, NULL)
ON CONFLICT (assignment_id) DO NOTHING;

SELECT setval('work_order_assignment_assignment_id_seq', (SELECT GREATEST(MAX(assignment_id), 4) FROM work_order_assignment));

-- 10. Inspection Results
INSERT INTO inspection_result (result_id, wo_id, checklist_id, done, condition, defect, work_done, entered_by, entered_at) VALUES
(1, 3, 4, TRUE, 'Thấu kính bám bụi nhẹ', 'Bụi mờ vi trường', 'Đã lau sạch bằng dung dịch cồn quang học', 4, CURRENT_TIMESTAMP - INTERVAL '1 day'),
(2, 3, 5, TRUE, 'Nguồn sáng LED sáng tốt', 'Không có lỗi', 'Kiểm tra độ chói đạt chuẩn', 4, CURRENT_TIMESTAMP - INTERVAL '1 day'),
(3, 3, 6, TRUE, 'Thước đo vi trường chính xác', 'Không có lỗi', 'Đã đối chiếu tiêu bản chuẩn', 4, CURRENT_TIMESTAMP - INTERVAL '1 day'),
(4, 4, 4, TRUE, 'Bình thường', 'Không phát hiện lỗi', 'Đã vệ sinh định kỳ', 4, CURRENT_TIMESTAMP - INTERVAL '80 days'),
(5, 4, 5, TRUE, 'Nguồn sáng tốt', 'Không có lỗi', 'Đã kiểm tra', 4, CURRENT_TIMESTAMP - INTERVAL '80 days')
ON CONFLICT (result_id) DO NOTHING;

SELECT setval('inspection_result_result_id_seq', (SELECT GREATEST(MAX(result_id), 5) FROM inspection_result));

-- 11. Work Order Acceptance for WO_C
INSERT INTO work_order_acceptance (acceptance_id, wo_id, decision, note, accepted_by, accepted_at) VALUES
(1, 4, 'PASS', 'Đạt yêu cầu kiểm tra định kỳ Q4/2025', 2, CURRENT_TIMESTAMP - INTERVAL '80 days')
ON CONFLICT (acceptance_id) DO NOTHING;

SELECT setval('work_order_acceptance_acceptance_id_seq', (SELECT GREATEST(MAX(acceptance_id), 1) FROM work_order_acceptance));

-- Close WO_C if it is still SUBMITTED
UPDATE maintenance_work_order 
SET status = 'CLOSED', closed_at = CURRENT_TIMESTAMP - INTERVAL '80 days'
WHERE wo_id = 4 AND status <> 'CLOSED';

-- 12. Incident Reports
INSERT INTO incident_report (incident_id, tracking_code, equipment_id, reporter_id, description, severity, status, created_at) VALUES
(1, 'INC-2026-0001', 5, 6, 'Máy phân tích phát ra tiếng ồn bất thường khi khởi động', 'HIGH', 'REPORTED', CURRENT_TIMESTAMP - INTERVAL '1 day'),
(2, 'INC-2026-0002', 6, 7, 'Màn hình hiển thị chập chờn, kết quả đo không ổn định', 'CRITICAL', 'CLASSIFIED', CURRENT_TIMESTAMP - INTERVAL '2 days')
ON CONFLICT (incident_id) DO NOTHING;

SELECT setval('incident_report_incident_id_seq', (SELECT GREATEST(MAX(incident_id), 2) FROM incident_report));
