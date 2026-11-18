package com.fpt.ems.be.config;

import com.fpt.ems.be.model.*;
import com.fpt.ems.be.model.enums.*;
import com.fpt.ems.be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Seed toàn bộ dữ liệu mẫu vào DB khi BE khởi động lần đầu.
 * Chạy sau RoleDataInitializer (Order 1) → Order 2.
 * Mỗi bước kiểm tra dữ liệu đã tồn tại trước khi insert (idempotent).
 *
 * Thứ tự seed (theo dependency):
 *   1. Roles          (đã xử lý bởi RoleDataInitializer)
 *   2. Users          (admin, manager, technician, equipment_user)
 *   3. Locations
 *   4. EquipmentTypes
 *   5. Equipment      (phụ thuộc: type, location)
 *   6. MaintenancePlans (phụ thuộc: equipment)
 *   7. MaintenanceChecklists (phụ thuộc: equipment_type)
 *   8. WorkOrders     (phụ thuộc: equipment, plan)
 *   9. WorkOrderAssignments (phụ thuộc: work_order, users)
 *  10. InspectionResults   (phụ thuộc: work_order, checklist, user)
 *  11. WorkOrderAcceptance (phụ thuộc: work_order, user)
 *  12. IncidentReports    (phụ thuộc: equipment, user)
 *
 * Mật khẩu mặc định: Demo@123456
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class FullDataSeeder implements ApplicationRunner {

    private static final String DEFAULT_PASSWORD = "Demo@123456";

    private final RoleRepository              roleRepository;
    private final AppUserRepository           userRepository;
    private final LocationRepository          locationRepository;
    private final EquipmentTypeRepository     equipmentTypeRepository;
    private final EquipmentRepository         equipmentRepository;
    private final MaintenancePlanRepository   planRepository;
    private final MaintenanceChecklistRepository checklistRepository;
    private final MaintenanceWorkOrderRepository workOrderRepository;
    private final WorkOrderAssignmentRepository  assignmentRepository;
    private final InspectionResultRepository     inspectionResultRepository;
    private final WorkOrderAcceptanceRepository  acceptanceRepository;
    private final IncidentReportRepository       incidentRepository;
    private final PasswordEncoder             passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("========== [FullDataSeeder] Bắt đầu seed dữ liệu mẫu ==========");

        // ── 1. Lấy roles (đã được seed bởi RoleDataInitializer) ──────────────
        Role adminRole   = getRole(RoleName.ADMIN);
        Role mgrRole     = getRole(RoleName.MANAGER);
        Role techRole    = getRole(RoleName.TECHNICIAN);
        Role userRole    = getRole(RoleName.EQUIPMENT_USER);

        // ── 2. Users ──────────────────────────────────────────────────────────
        log.info("[FullDataSeeder] >> Seed Users...");
        String pwd = passwordEncoder.encode(DEFAULT_PASSWORD);

        AppUser admin  = seedUser("ADMIN01", "admin01@ems.fpt.edu.vn", "System Administrator",   adminRole, pwd);
        AppUser mgr1   = seedUser("MGR01",   "mgr01@ems.fpt.edu.vn",   "Nguyen Van Manager",      mgrRole,  pwd);
        seedUser(                 "MGR02",   "mgr02@ems.fpt.edu.vn",   "Tran Thi Manager",        mgrRole,  pwd);
        AppUser techA  = seedUser("TECH_A",  "tech_a@ems.fpt.edu.vn",  "Le Van Technician A",     techRole, pwd);
        AppUser techB  = seedUser("TECH_B",  "tech_b@ems.fpt.edu.vn",  "Pham Van Technician B",   techRole, pwd);
        AppUser user1  = seedUser("USER01",  "user01@ems.fpt.edu.vn",  "Hoang User One",          userRole, pwd);
        AppUser user2  = seedUser("USER02",  "user02@ems.fpt.edu.vn",  "Vu User Two",             userRole, pwd);

        // ── 3. Locations ──────────────────────────────────────────────────────
        log.info("[FullDataSeeder] >> Seed Locations...");
        Location locA = seedLocation("P.101", "Khu A", "Phòng Thí Nghiệm Cơ Điện Tử");
        Location locB = seedLocation("P.202", "Khu B", "Phòng Thí Nghiệm Hóa Sinh");
        Location locC = seedLocation("P.303", "Khu C", "Trung Tâm Mạng & Máy Tính");

        // ── 4. Equipment Types ────────────────────────────────────────────────
        log.info("[FullDataSeeder] >> Seed Equipment Types...");
        EquipmentType typeLyTam   = seedEquipmentType("Máy Ly Tâm",
                "Máy ly tâm phục vụ phân tách mẫu hóa sinh trong lab");
        EquipmentType typeKinhHV  = seedEquipmentType("Kính Hiển Vi Quang Học",
                "Kính hiển vi độ phóng đại cao cho nghiên cứu vi sinh");
        EquipmentType typeQuangPho = seedEquipmentType("Máy Phân Tích Quang Phổ",
                "Thiết bị đo độ hấp thụ quang và phân tích mẫu vật");

        // ── 5. Equipment ──────────────────────────────────────────────────────
        log.info("[FullDataSeeder] >> Seed Equipment...");
        Equipment eq01  = seedEquipment("EQ01", "Eppendorf", "5810R",      "SN-EP-5810-01",  EquipmentStatus.ACTIVE,          typeLyTam,    locA);
        Equipment eq02  = seedEquipment("EQ02", "Thermo",    "SL16R",      "SN-TH-16R-02",   EquipmentStatus.ACTIVE,          typeLyTam,    locA);
        Equipment eq03  = seedEquipment("EQ03", "Olympus",   "CX23",       "SN-OL-CX23-03",  EquipmentStatus.ACTIVE,          typeKinhHV,   locB);
        Equipment eq04  = seedEquipment("EQ04", "Nikon",     "Eclipse",    "SN-NK-ECL-04",   EquipmentStatus.ACTIVE,          typeKinhHV,   locB);
        Equipment eq05  = seedEquipment("EQ05", "Shimadzu",  "UV-1900i",   "SN-SH-1900-05",  EquipmentStatus.ACTIVE,          typeQuangPho, locC);
        Equipment eq06  = seedEquipment("EQ06", "Agilent",   "Cary 60",    "SN-AG-C60-06",   EquipmentStatus.ACTIVE,          typeQuangPho, locC);
        Equipment eq07  = seedEquipment("EQ07", "Eppendorf", "MiniSpin",   "SN-EP-MS-07",    EquipmentStatus.ACTIVE,          typeLyTam,    locA);
        Equipment eq08  = seedEquipment("EQ08", "Olympus",   "BX43",       "SN-OL-BX43-08",  EquipmentStatus.ACTIVE,          typeKinhHV,   locB);
        Equipment eq09  = seedEquipment("EQ09", "Perkin",    "Lambda 365", "SN-PK-L36-09",   EquipmentStatus.ACTIVE,          typeQuangPho, locC);
        Equipment eq10  = seedEquipment("EQ10", "Thermo",    "BioMate",    "SN-TH-BM-10",    EquipmentStatus.OUT_OF_SERVICE,  typeQuangPho, locC);

        // ── 6. Maintenance Plans ──────────────────────────────────────────────
        log.info("[FullDataSeeder] >> Seed Maintenance Plans...");
        LocalDate today = LocalDate.now();
        MaintenancePlan plan1  = seedPlan(eq01, 90,  today.minusDays(100), today.minusDays(10));
        MaintenancePlan plan2  = seedPlan(eq02, 30,  today.minusDays(27),  today.plusDays(3));
        MaintenancePlan plan3  = seedPlan(eq03, 90,  today.minusDays(30),  today.plusDays(60));
        MaintenancePlan plan4  = seedPlan(eq04, 180, today.minusDays(180), today.minusDays(1));
        seedPlan(eq05, 90,  today.minusDays(85),  today.plusDays(5));
        seedPlan(eq06, 30,  today.minusDays(10),  today.plusDays(20));
        seedPlan(eq07, 90,  today.minusDays(20),  today.plusDays(70));
        seedPlan(eq08, 180, today.minusDays(50),  today.plusDays(130));
        seedPlan(eq09, 90,  today.minusDays(88),  today.plusDays(2));
        seedPlan(eq10, 90,  today.minusDays(95),  today.minusDays(5));

        // ── 7. Maintenance Checklists ─────────────────────────────────────────
        log.info("[FullDataSeeder] >> Seed Maintenance Checklists...");
        // Máy Ly Tâm
        MaintenanceChecklist cl1 = seedChecklist(typeLyTam,   "Kiểm tra độ cân bằng rotor và độ rơ trục quay",        true);
        MaintenanceChecklist cl2 = seedChecklist(typeLyTam,   "Vệ sinh khoang ly tâm và bôi trơn đệm cao su nắp",     true);
        seedChecklist(typeLyTam,   "Kiểm tra tốc độ quay bằng máy đo quang học",             false);
        // Kính Hiển Vi
        MaintenanceChecklist cl4 = seedChecklist(typeKinhHV,  "Lau sạch thấu kính và thị kính bằng cồn chuyên dụng",  true);
        MaintenanceChecklist cl5 = seedChecklist(typeKinhHV,  "Kiểm tra độ hội tụ và nguồn sáng LED/Halogen",         true);
        MaintenanceChecklist cl6 = seedChecklist(typeKinhHV,  "Hiệu chuẩn thước đo vi trường",                        false);
        // Máy Phân Tích Quang Phổ
        seedChecklist(typeQuangPho, "Kiểm tra nguồn đèn Deuterium / Tungsten",                true);
        seedChecklist(typeQuangPho, "Hiệu chuẩn bước sóng bằng kính lọc chuẩn Holmium",       true);
        seedChecklist(typeQuangPho, "Vệ sinh buồng đo mẫu cuvet",                             true);

        // ── 8. Work Orders ────────────────────────────────────────────────────
        log.info("[FullDataSeeder] >> Seed Work Orders...");
        Instant now = Instant.now();
        MaintenanceWorkOrder woA = seedWorkOrder("WO_A", eq01, plan1, WorkOrderType.PERIODIC, "2026-Q1", WorkOrderStatus.ASSIGNED,  now.minus(3, ChronoUnit.DAYS),  null);
        MaintenanceWorkOrder woB = seedWorkOrder("WO_B", eq02, plan2, WorkOrderType.PERIODIC, "2026-Q1", WorkOrderStatus.ASSIGNED,  now.minus(2, ChronoUnit.DAYS),  null);
        MaintenanceWorkOrder woP = seedWorkOrder("WO_P", eq03, plan3, WorkOrderType.PERIODIC, "2026-Q1", WorkOrderStatus.SUBMITTED, now.minus(5, ChronoUnit.DAYS),  null);
        MaintenanceWorkOrder woC = seedWorkOrder("WO_C", eq04, plan4, WorkOrderType.PERIODIC, "2025-Q4", WorkOrderStatus.SUBMITTED, now.minus(90, ChronoUnit.DAYS), null);

        // ── 9. Work Order Assignments ─────────────────────────────────────────
        log.info("[FullDataSeeder] >> Seed Work Order Assignments...");
        seedAssignment(woA, techA, mgr1, now.minus(3,  ChronoUnit.DAYS));
        seedAssignment(woB, techB, mgr1, now.minus(2,  ChronoUnit.DAYS));
        seedAssignment(woP, techA, mgr1, now.minus(5,  ChronoUnit.DAYS));
        seedAssignment(woC, techA, mgr1, now.minus(90, ChronoUnit.DAYS));

        // ── 10. Inspection Results ────────────────────────────────────────────
        log.info("[FullDataSeeder] >> Seed Inspection Results...");
        // WO_P (Kính hiển vi CX23)
        seedInspection(woP, cl4, true,  "Thấu kính bám bụi nhẹ",        "Bụi mờ vi trường",  "Đã lau sạch bằng dung dịch cồn quang học",  techA, now.minus(1, ChronoUnit.DAYS));
        seedInspection(woP, cl5, true,  "Nguồn sáng LED sáng tốt",       "Không có lỗi",      "Kiểm tra độ chói đạt chuẩn",                techA, now.minus(1, ChronoUnit.DAYS));
        seedInspection(woP, cl6, true,  "Thước đo vi trường chính xác",  "Không có lỗi",      "Đã đối chiếu tiêu bản chuẩn",               techA, now.minus(1, ChronoUnit.DAYS));
        // WO_C (Kính hiển vi Eclipse)
        seedInspection(woC, cl4, true,  "Bình thường",                   "Không phát hiện lỗi", "Đã vệ sinh định kỳ",                      techA, now.minus(80, ChronoUnit.DAYS));
        seedInspection(woC, cl5, true,  "Nguồn sáng tốt",                "Không có lỗi",        "Đã kiểm tra",                             techA, now.minus(80, ChronoUnit.DAYS));

        // ── 11. Work Order Acceptance ─────────────────────────────────────────
        log.info("[FullDataSeeder] >> Seed Work Order Acceptance...");
        boolean acceptanceExists = !acceptanceRepository.findByWorkOrder_WoIdOrderByAcceptedAtDesc(woC.getWoId()).isEmpty();
        if (!acceptanceExists) {
            acceptanceRepository.save(WorkOrderAcceptance.builder()
                .workOrder(woC)
                .decision(AcceptanceDecision.PASS)
                .note("Đạt yêu cầu kiểm tra định kỳ Q4/2025")
                .acceptedBy(mgr1)
                .acceptedAt(now.minus(80, ChronoUnit.DAYS))
                .build());
            log.info("[FullDataSeeder]   ✅ Acceptance cho WO_C đã được tạo");

            // Chuyển WO_C sang CLOSED
            woC.setStatus(WorkOrderStatus.CLOSED);
            woC.setClosedAt(now.minus(80, ChronoUnit.DAYS));
            workOrderRepository.save(woC);
            log.info("[FullDataSeeder]   ✅ WO_C → CLOSED");
        } else {
            log.debug("[FullDataSeeder]   Acceptance WO_C đã tồn tại, bỏ qua.");
        }

        // ── 12. Incident Reports ──────────────────────────────────────────────
        log.info("[FullDataSeeder] >> Seed Incident Reports...");
        seedIncident("INC-2026-0001", eq05, user1,
                "Máy phân tích phát ra tiếng ồn bất thường khi khởi động",
                IncidentSeverity.HIGH, IncidentStatus.REPORTED, now.minus(1, ChronoUnit.DAYS));
        seedIncident("INC-2026-0002", eq06, user2,
                "Màn hình hiển thị chập chờn, kết quả đo không ổn định",
                IncidentSeverity.CRITICAL, IncidentStatus.CLASSIFIED, now.minus(2, ChronoUnit.DAYS));

        log.info("========== [FullDataSeeder] Seed hoàn tất. Mật khẩu mặc định: {} ==========", DEFAULT_PASSWORD);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPER METHODS
    // ══════════════════════════════════════════════════════════════════════════

    private Role getRole(RoleName roleName) {
        return roleRepository.findByName(roleName)
            .orElseThrow(() -> new IllegalStateException(
                "[FullDataSeeder] Role không tồn tại: " + roleName));
    }

    private AppUser seedUser(String username, String email, String fullName, Role role, String encodedPwd) {
        if (userRepository.existsByUsername(username)) {
            log.debug("[FullDataSeeder]   User '{}' đã tồn tại.", username);
            return userRepository.findByUsername(username).orElseThrow();
        }
        AppUser user = userRepository.save(AppUser.builder()
            .username(username)
            .email(email)
            .passwordHash(encodedPwd)
            .fullName(fullName)
            .status(UserStatus.ACTIVE)
            .role(role)
            .failedLoginCount(0)
            .build());
        log.info("[FullDataSeeder]   ✅ User: {} ({})", username, role.getName());
        return user;
    }

    private Location seedLocation(String room, String area, String department) {
        if (locationRepository.existsByRoomAndArea(room, area)) {
            log.debug("[FullDataSeeder]   Location '{}/{}' đã tồn tại.", room, area);
            return locationRepository.findByRoomAndArea(room, area).orElseThrow();
        }
        Location loc = locationRepository.save(Location.builder()
            .room(room).area(area).department(department).build());
        log.info("[FullDataSeeder]   ✅ Location: {} - {}", room, area);
        return loc;
    }

    private EquipmentType seedEquipmentType(String name, String description) {
        if (equipmentTypeRepository.existsByName(name)) {
            log.debug("[FullDataSeeder]   EquipmentType '{}' đã tồn tại.", name);
            return equipmentTypeRepository.findByName(name).orElseThrow();
        }
        EquipmentType type = equipmentTypeRepository.save(EquipmentType.builder()
            .name(name).description(description).build());
        log.info("[FullDataSeeder]   ✅ EquipmentType: {}", name);
        return type;
    }

    private Equipment seedEquipment(String code, String brand, String model, String serial,
                                    EquipmentStatus status, EquipmentType type, Location location) {
        if (equipmentRepository.existsByCode(code)) {
            log.debug("[FullDataSeeder]   Equipment '{}' đã tồn tại.", code);
            return equipmentRepository.findByCode(code).orElseThrow();
        }
        Equipment eq = equipmentRepository.save(Equipment.builder()
            .code(code).brand(brand).model(model).serial(serial)
            .status(status).type(type).location(location).build());
        log.info("[FullDataSeeder]   ✅ Equipment: {} ({} {})", code, brand, model);
        return eq;
    }

    private MaintenancePlan seedPlan(Equipment equipment, int cycleDays,
                                     LocalDate lastCompleted, LocalDate nextDue) {
        if (planRepository.findByEquipment_EquipmentId(equipment.getEquipmentId()).isPresent()) {
            log.debug("[FullDataSeeder]   MaintenancePlan cho '{}' đã tồn tại.", equipment.getCode());
            return planRepository.findByEquipment_EquipmentId(equipment.getEquipmentId()).orElseThrow();
        }
        MaintenancePlan plan = planRepository.save(MaintenancePlan.builder()
            .equipment(equipment).cycleDays(cycleDays)
            .lastCompletedDate(lastCompleted).nextDueDate(nextDue).build());
        log.info("[FullDataSeeder]   ✅ Plan: {} ({}d, next: {})", equipment.getCode(), cycleDays, nextDue);
        return plan;
    }

    private MaintenanceChecklist seedChecklist(EquipmentType type, String item, boolean required) {
        // Kiểm tra theo item + type để tránh trùng
        boolean exists = checklistRepository.findByEquipmentType_TypeId(type.getTypeId())
            .stream().anyMatch(c -> c.getItem().equals(item));
        if (exists) {
            log.debug("[FullDataSeeder]   Checklist '{}...' đã tồn tại.", item.substring(0, Math.min(20, item.length())));
            return checklistRepository.findByEquipmentType_TypeId(type.getTypeId())
                .stream().filter(c -> c.getItem().equals(item)).findFirst().orElseThrow();
        }
        MaintenanceChecklist cl = checklistRepository.save(MaintenanceChecklist.builder()
            .equipmentType(type).item(item).isRequired(required).build());
        log.info("[FullDataSeeder]   ✅ Checklist [{}]: {}...", type.getName(), item.substring(0, Math.min(30, item.length())));
        return cl;
    }

    private MaintenanceWorkOrder seedWorkOrder(String woCode, Equipment equipment, MaintenancePlan plan,
                                               WorkOrderType type, String period,
                                               WorkOrderStatus status, Instant createdAt, Instant closedAt) {
        if (workOrderRepository.existsByWoCode(woCode)) {
            log.debug("[FullDataSeeder]   WorkOrder '{}' đã tồn tại.", woCode);
            return workOrderRepository.findByWoCode(woCode).orElseThrow();
        }
        MaintenanceWorkOrder wo = workOrderRepository.save(MaintenanceWorkOrder.builder()
            .woCode(woCode).equipment(equipment).plan(plan)
            .type(type).period(period).status(status)
            .createdAt(createdAt).closedAt(closedAt).build());
        log.info("[FullDataSeeder]   ✅ WorkOrder: {} ({})", woCode, status);
        return wo;
    }

    private void seedAssignment(MaintenanceWorkOrder wo, AppUser technician, AppUser assignedBy, Instant assignedAt) {
        boolean exists = assignmentRepository.existsByWorkOrder_WoIdAndTechnician_UserId(
            wo.getWoId(), technician.getUserId());
        if (exists) {
            log.debug("[FullDataSeeder]   Assignment {}->{} đã tồn tại.", wo.getWoCode(), technician.getUsername());
            return;
        }
        assignmentRepository.save(WorkOrderAssignment.builder()
            .workOrder(wo).technician(technician).assignedBy(assignedBy)
            .assignedAt(assignedAt).isCurrent(true).build());
        log.info("[FullDataSeeder]   ✅ Assignment: {} → {}", wo.getWoCode(), technician.getUsername());
    }

    private void seedInspection(MaintenanceWorkOrder wo, MaintenanceChecklist checklist,
                                 boolean done, String condition, String defect, String workDone,
                                 AppUser enteredBy, Instant enteredAt) {
        boolean exists = inspectionResultRepository
            .findByWorkOrder_WoIdAndChecklist_ChecklistId(wo.getWoId(), checklist.getChecklistId())
            .isPresent();
        if (exists) {
            log.debug("[FullDataSeeder]   InspectionResult {}/{} đã tồn tại.", wo.getWoCode(), checklist.getChecklistId());
            return;
        }
        inspectionResultRepository.save(InspectionResult.builder()
            .workOrder(wo).checklist(checklist).done(done)
            .condition(condition).defect(defect).workDone(workDone)
            .enteredBy(enteredBy).enteredAt(enteredAt).build());
        log.info("[FullDataSeeder]   ✅ Inspection: {} / checklist#{}", wo.getWoCode(), checklist.getChecklistId());
    }

    private void seedIncident(String trackingCode, Equipment equipment, AppUser reporter,
                               String description, IncidentSeverity severity,
                               IncidentStatus status, Instant createdAt) {
        if (incidentRepository.existsByTrackingCode(trackingCode)) {
            log.debug("[FullDataSeeder]   Incident '{}' đã tồn tại.", trackingCode);
            return;
        }
        incidentRepository.save(IncidentReport.builder()
            .trackingCode(trackingCode).equipment(equipment).reporter(reporter)
            .description(description).severity(severity).status(status)
            .createdAt(createdAt).build());
        log.info("[FullDataSeeder]   ✅ Incident: {} ({}/{})", trackingCode, severity, status);
    }
}
