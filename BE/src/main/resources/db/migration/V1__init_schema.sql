-- V1__init_schema.sql
-- Equipment Maintenance & Service Scheduling System (P05)
-- PostgreSQL DDL for 16 tables: 12 SRS tables + 4 security enhancement tables

-- 1. role
CREATE TABLE role (
    role_id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- 2. app_user
CREATE TABLE app_user (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    role_id INT NOT NULL REFERENCES role(role_id) ON DELETE RESTRICT,
    failed_login_count INT NOT NULL DEFAULT 0,
    last_failed_at TIMESTAMP WITH TIME ZONE NULL,
    locked_until TIMESTAMP WITH TIME ZONE NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_app_user_status CHECK (status IN ('ACTIVE', 'LOCKED'))
);

-- 3. location
CREATE TABLE location (
    location_id BIGSERIAL PRIMARY KEY,
    room VARCHAR(50) NOT NULL,
    area VARCHAR(100) NOT NULL,
    department VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_location_room_area CHECK (length(trim(room)) > 0 AND length(trim(area)) > 0)
);

-- 4. equipment_type
CREATE TABLE equipment_type (
    type_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_equipment_type_name CHECK (length(trim(name)) > 0)
);

-- 5. equipment
CREATE TABLE equipment (
    equipment_id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    brand VARCHAR(100),
    model VARCHAR(100),
    serial VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    type_id BIGINT NOT NULL REFERENCES equipment_type(type_id) ON DELETE RESTRICT,
    location_id BIGINT NOT NULL REFERENCES location(location_id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_equipment_code CHECK (length(trim(code)) > 0),
    CONSTRAINT chk_equipment_status CHECK (status IN ('ACTIVE', 'UNDER_MAINTENANCE', 'OUT_OF_SERVICE'))
);

-- 6. maintenance_plan
CREATE TABLE maintenance_plan (
    plan_id BIGSERIAL PRIMARY KEY,
    equipment_id BIGINT NOT NULL UNIQUE REFERENCES equipment(equipment_id) ON DELETE RESTRICT,
    cycle_days INT NOT NULL,
    last_completed_date DATE,
    next_due_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_plan_cycle_days CHECK (cycle_days IN (30, 90, 180)),
    CONSTRAINT chk_plan_dates CHECK (next_due_date IS NULL OR last_completed_date IS NULL OR next_due_date >= last_completed_date)
);

-- 7. maintenance_checklist
CREATE TABLE maintenance_checklist (
    checklist_id BIGSERIAL PRIMARY KEY,
    type_id BIGINT NOT NULL REFERENCES equipment_type(type_id) ON DELETE RESTRICT,
    item VARCHAR(255) NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_checklist_item CHECK (length(trim(item)) > 0)
);

-- 8. incident_report
CREATE TABLE incident_report (
    incident_id BIGSERIAL PRIMARY KEY,
    tracking_code VARCHAR(50) NOT NULL UNIQUE,
    equipment_id BIGINT NOT NULL REFERENCES equipment(equipment_id) ON DELETE RESTRICT,
    reporter_id BIGINT NOT NULL REFERENCES app_user(user_id) ON DELETE RESTRICT,
    description TEXT NOT NULL,
    severity VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'REPORTED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT chk_incident_description CHECK (length(trim(description)) > 0),
    CONSTRAINT chk_incident_severity CHECK (severity IS NULL OR severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_incident_status CHECK (status IN ('REPORTED', 'CLASSIFIED', 'ASSIGNED', 'IN_REPAIR', 'RESOLVED', 'CLOSED'))
);

-- 9. maintenance_work_order
CREATE TABLE maintenance_work_order (
    wo_id BIGSERIAL PRIMARY KEY,
    wo_code VARCHAR(50) NOT NULL UNIQUE,
    equipment_id BIGINT NOT NULL REFERENCES equipment(equipment_id) ON DELETE RESTRICT,
    plan_id BIGINT REFERENCES maintenance_plan(plan_id) ON DELETE RESTRICT,
    incident_id BIGINT REFERENCES incident_report(incident_id) ON DELETE RESTRICT,
    type VARCHAR(20) NOT NULL,
    period VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT chk_wo_type CHECK (type IN ('PERIODIC', 'REPAIR')),
    CONSTRAINT chk_wo_status CHECK (status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS', 'SUBMITTED', 'CLOSED')),
    CONSTRAINT chk_wo_periodic_plan CHECK (type <> 'PERIODIC' OR plan_id IS NOT NULL)
);

-- 10. work_order_assignment
CREATE TABLE work_order_assignment (
    assignment_id BIGSERIAL PRIMARY KEY,
    wo_id BIGINT NOT NULL REFERENCES maintenance_work_order(wo_id) ON DELETE RESTRICT,
    technician_id BIGINT NOT NULL REFERENCES app_user(user_id) ON DELETE RESTRICT,
    assigned_by BIGINT NOT NULL REFERENCES app_user(user_id) ON DELETE RESTRICT,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    unassigned_at TIMESTAMP WITH TIME ZONE NULL
);

-- 11. inspection_result
CREATE TABLE inspection_result (
    result_id BIGSERIAL PRIMARY KEY,
    wo_id BIGINT NOT NULL REFERENCES maintenance_work_order(wo_id) ON DELETE RESTRICT,
    checklist_id BIGINT NOT NULL REFERENCES maintenance_checklist(checklist_id) ON DELETE RESTRICT,
    done BOOLEAN NOT NULL DEFAULT FALSE,
    condition VARCHAR(255),
    defect TEXT,
    work_done TEXT,
    entered_by BIGINT REFERENCES app_user(user_id) ON DELETE RESTRICT,
    entered_at TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT uq_inspection_result_wo_checklist UNIQUE (wo_id, checklist_id)
);

-- 12. maintenance_history (append-only)
CREATE TABLE maintenance_history (
    history_id BIGSERIAL PRIMARY KEY,
    equipment_id BIGINT NOT NULL REFERENCES equipment(equipment_id) ON DELETE RESTRICT,
    wo_id BIGINT REFERENCES maintenance_work_order(wo_id) ON DELETE RESTRICT,
    action VARCHAR(50) NOT NULL,
    old_status VARCHAR(50),
    new_status VARCHAR(50),
    performed_by BIGINT REFERENCES app_user(user_id) ON DELETE RESTRICT,
    performed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note TEXT
);

-- 13. work_order_acceptance (append-only)
CREATE TABLE work_order_acceptance (
    acceptance_id BIGSERIAL PRIMARY KEY,
    wo_id BIGINT NOT NULL REFERENCES maintenance_work_order(wo_id) ON DELETE RESTRICT,
    decision VARCHAR(10) NOT NULL,
    note TEXT,
    accepted_by BIGINT NOT NULL REFERENCES app_user(user_id) ON DELETE RESTRICT,
    accepted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_acceptance_decision CHECK (decision IN ('PASS', 'FAIL')),
    CONSTRAINT chk_acceptance_fail_note CHECK (decision <> 'FAIL' OR (note IS NOT NULL AND length(trim(note)) > 0))
);

-- 14. work_order_adjustment (append-only, only for CLOSED WO)
CREATE TABLE work_order_adjustment (
    adjustment_id BIGSERIAL PRIMARY KEY,
    wo_id BIGINT NOT NULL REFERENCES maintenance_work_order(wo_id) ON DELETE RESTRICT,
    reason TEXT NOT NULL,
    change_detail JSONB,
    created_by BIGINT NOT NULL REFERENCES app_user(user_id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_adjustment_reason CHECK (length(trim(reason)) > 0)
);

-- 15. status_change_request
CREATE TABLE status_change_request (
    request_id BIGSERIAL PRIMARY KEY,
    equipment_id BIGINT NOT NULL REFERENCES equipment(equipment_id) ON DELETE RESTRICT,
    wo_id BIGINT REFERENCES maintenance_work_order(wo_id) ON DELETE RESTRICT,
    proposed_status VARCHAR(50) NOT NULL DEFAULT 'OUT_OF_SERVICE',
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    proposed_by BIGINT NOT NULL REFERENCES app_user(user_id) ON DELETE RESTRICT,
    decided_by BIGINT REFERENCES app_user(user_id) ON DELETE RESTRICT,
    proposed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT chk_scr_proposed_status CHECK (proposed_status = 'OUT_OF_SERVICE'),
    CONSTRAINT chk_scr_status CHECK (status IN ('PENDING', 'CONFIRMED', 'REJECTED')),
    CONSTRAINT chk_scr_reason CHECK (length(trim(reason)) > 0)
);

-- 16. audit_log (append-only)
CREATE TABLE audit_log (
    log_id BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor_id BIGINT REFERENCES app_user(user_id) ON DELETE RESTRICT,
    actor_role VARCHAR(50),
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(50),
    target_id VARCHAR(50),
    outcome VARCHAR(20) NOT NULL,
    reason_code VARCHAR(50),
    old_value JSONB,
    new_value JSONB,
    request_id VARCHAR(100),
    source_ip VARCHAR(50),
    CONSTRAINT chk_audit_outcome CHECK (outcome IN ('SUCCESS', 'DENIED', 'FAILED', 'ALERT'))
);

-- Partial Unique Indexes
-- UQ-05: Chống 2 WO định kỳ còn mở cùng 1 kỳ của 1 thiết bị
CREATE UNIQUE INDEX uq_periodic_wo_open 
    ON maintenance_work_order (equipment_id, period) 
    WHERE type = 'PERIODIC' AND status <> 'CLOSED';

-- UQ-06: Mỗi Work Order chỉ có tối đa 1 phân công có hiệu lực hiện tại
CREATE UNIQUE INDEX uq_wo_current_assignment 
    ON work_order_assignment (wo_id) 
    WHERE is_current = TRUE;

-- Performance Indexes
CREATE INDEX idx_equipment_lookup ON equipment (code, status, type_id, location_id);
CREATE INDEX idx_work_order_equip_status ON maintenance_work_order (equipment_id, status);
CREATE INDEX idx_history_equip_time ON maintenance_history (equipment_id, performed_at DESC);
CREATE INDEX idx_plan_next_due ON maintenance_plan (next_due_date);
CREATE INDEX idx_audit_log_query ON audit_log (occurred_at DESC, actor_id, action);
CREATE INDEX idx_incident_status ON incident_report (status, equipment_id);

