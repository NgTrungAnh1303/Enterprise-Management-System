-- V2__constraints_triggers.sql
-- Equipment Maintenance & Service Scheduling System (P05)
-- Database security roles, permissions, and defense-in-depth triggers

-- 1. Database user role: app_user (Least privilege principle)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'app_user') THEN
        CREATE ROLE app_user WITH LOGIN PASSWORD '${app_user_password}';
    ELSE
        ALTER ROLE app_user WITH PASSWORD '${app_user_password}';
    END IF;
END
$$;

-- Grant schema usage
GRANT USAGE ON SCHEMA public TO app_user;

-- Grant SELECT, INSERT, UPDATE on operational tables
GRANT SELECT, INSERT, UPDATE ON 
    app_user, 
    location, 
    equipment_type, 
    equipment, 
    maintenance_plan, 
    maintenance_checklist, 
    maintenance_work_order, 
    work_order_assignment, 
    inspection_result, 
    incident_report, 
    status_change_request 
TO app_user;

-- Grant SELECT, INSERT ONLY on append-only tables & role
GRANT SELECT, INSERT ON 
    maintenance_history, 
    work_order_acceptance, 
    work_order_adjustment, 
    audit_log,
    role 
TO app_user;

-- Explicitly revoke UPDATE and DELETE on append-only tables
REVOKE UPDATE, DELETE ON maintenance_history FROM app_user;
REVOKE UPDATE, DELETE ON audit_log FROM app_user;
REVOKE UPDATE, DELETE ON work_order_acceptance FROM app_user;
REVOKE UPDATE, DELETE ON work_order_adjustment FROM app_user;

-- Explicitly revoke DELETE on all tables for app_user (no physical deletion)
REVOKE DELETE ON ALL TABLES IN SCHEMA public FROM app_user;

-- Grant sequence privileges for ID generation
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_user;


-- 2. Triggers for Append-Only tables (Defense-in-depth)
-- Blocks any UPDATE or DELETE even if executed by privileged migration_user
CREATE OR REPLACE FUNCTION fn_block_append_only_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Table % is append-only: UPDATE and DELETE operations are strictly prohibited.', TG_TABLE_NAME
        USING ERRCODE = '55000';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_block_history_mutation
    BEFORE UPDATE OR DELETE ON maintenance_history
    FOR EACH ROW EXECUTE FUNCTION fn_block_append_only_mutation();

CREATE TRIGGER trg_block_audit_mutation
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION fn_block_append_only_mutation();

CREATE TRIGGER trg_block_acceptance_mutation
    BEFORE UPDATE OR DELETE ON work_order_acceptance
    FOR EACH ROW EXECUTE FUNCTION fn_block_append_only_mutation();

CREATE TRIGGER trg_block_adjustment_mutation
    BEFORE UPDATE OR DELETE ON work_order_adjustment
    FOR EACH ROW EXECUTE FUNCTION fn_block_append_only_mutation();


-- 3. Triggers for Work Order CLOSED Immutability (IA-EM03 Defense-in-depth)
-- Work Order: block modification if OLD.status = 'CLOSED'
CREATE OR REPLACE FUNCTION fn_block_closed_work_order_mutation()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF OLD.status = 'CLOSED' THEN
            RAISE EXCEPTION 'Cannot DELETE work order: Work Order % is CLOSED and immutable.', OLD.wo_code
                USING ERRCODE = '55000';
        END IF;
        RETURN OLD;
    ELSIF TG_OP = 'UPDATE' THEN
        IF OLD.status = 'CLOSED' THEN
            RAISE EXCEPTION 'Cannot UPDATE work order: Work Order % is CLOSED and immutable.', OLD.wo_code
                USING ERRCODE = '55000';
        END IF;
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_block_closed_wo_mutation
    BEFORE UPDATE OR DELETE ON maintenance_work_order
    FOR EACH ROW EXECUTE FUNCTION fn_block_closed_work_order_mutation();


-- Inspection Result: block INSERT, UPDATE, DELETE if associated Work Order is CLOSED
CREATE OR REPLACE FUNCTION fn_block_inspection_result_on_closed_wo()
RETURNS TRIGGER AS $$
DECLARE
    v_wo_status VARCHAR(20);
    v_target_wo_id BIGINT;
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_target_wo_id := OLD.wo_id;
    ELSE
        v_target_wo_id := NEW.wo_id;
    END IF;

    SELECT status INTO v_wo_status FROM maintenance_work_order WHERE wo_id = v_target_wo_id;

    IF v_wo_status = 'CLOSED' THEN
        RAISE EXCEPTION 'Cannot % inspection_result: Work Order % is CLOSED and immutable.', TG_OP, v_target_wo_id
            USING ERRCODE = '55000';
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_block_inspection_result_on_closed_wo
    BEFORE INSERT OR UPDATE OR DELETE ON inspection_result
    FOR EACH ROW EXECUTE FUNCTION fn_block_inspection_result_on_closed_wo();


-- Work Order Assignment: block INSERT, UPDATE, DELETE if associated Work Order is CLOSED
CREATE OR REPLACE FUNCTION fn_block_assignment_on_closed_wo()
RETURNS TRIGGER AS $$
DECLARE
    v_wo_status VARCHAR(20);
    v_target_wo_id BIGINT;
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_target_wo_id := OLD.wo_id;
    ELSE
        v_target_wo_id := NEW.wo_id;
    END IF;

    SELECT status INTO v_wo_status FROM maintenance_work_order WHERE wo_id = v_target_wo_id;

    IF v_wo_status = 'CLOSED' THEN
        RAISE EXCEPTION 'Cannot % work_order_assignment: Work Order % is CLOSED and immutable.', TG_OP, v_target_wo_id
            USING ERRCODE = '55000';
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_block_assignment_on_closed_wo
    BEFORE INSERT OR UPDATE OR DELETE ON work_order_assignment
    FOR EACH ROW EXECUTE FUNCTION fn_block_assignment_on_closed_wo();

