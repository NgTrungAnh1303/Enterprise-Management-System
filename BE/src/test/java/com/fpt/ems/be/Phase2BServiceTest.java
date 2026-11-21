package com.fpt.ems.be;

import com.fpt.ems.be.dto.dashboard.AlertResponse;
import com.fpt.ems.be.dto.dashboard.DashboardResponse;
import com.fpt.ems.be.dto.incident.ClassifyIncidentRequest;
import com.fpt.ems.be.dto.incident.IncidentCreateRequest;
import com.fpt.ems.be.dto.incident.IncidentProgressRequest;
import com.fpt.ems.be.dto.incident.IncidentResponse;
import com.fpt.ems.be.dto.statusrequest.CreateStatusRequestDto;
import com.fpt.ems.be.dto.statusrequest.StatusChangeRequestResponse;
import com.fpt.ems.be.dto.statusrequest.StatusRequestDecisionDto;
import com.fpt.ems.be.dto.workorder.*;
import com.fpt.ems.be.exception.AccessDeniedBusinessException;
import com.fpt.ems.be.exception.BusinessException;
import com.fpt.ems.be.model.*;
import com.fpt.ems.be.model.enums.*;
import com.fpt.ems.be.repository.*;
import com.fpt.ems.be.security.AuthorizationPolicy;
import com.fpt.ems.be.service.*;
import com.fpt.ems.be.statemachine.EquipmentStateMachine;
import com.fpt.ems.be.statemachine.IncidentStateMachine;
import com.fpt.ems.be.statemachine.WorkOrderStateMachine;
import com.fpt.ems.be.util.DueLabelCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Phase2BServiceTest {

    @Mock private MaintenanceWorkOrderRepository workOrderRepository;
    @Mock private WorkOrderAssignmentRepository assignmentRepository;
    @Mock private InspectionResultRepository inspectionResultRepository;
    @Mock private MaintenanceChecklistRepository checklistRepository;
    @Mock private EquipmentRepository equipmentRepository;
    @Mock private MaintenancePlanRepository planRepository;
    @Mock private IncidentReportRepository incidentRepository;
    @Mock private AppUserRepository userRepository;
    @Mock private WorkOrderAcceptanceRepository acceptanceRepository;
    @Mock private WorkOrderAdjustmentRepository adjustmentRepository;
    @Mock private StatusChangeRequestRepository statusChangeRequestRepository;
    @Mock private MaintenanceHistoryRepository historyRepository;

    @Mock private MaintenanceHistoryService historyService;
    @Mock private AuditService auditService;
    @Mock private AuthorizationPolicy authorizationPolicy;

    @Spy private WorkOrderStateMachine workOrderStateMachine = new WorkOrderStateMachine();
    @Spy private EquipmentStateMachine equipmentStateMachine = new EquipmentStateMachine();
    @Spy private IncidentStateMachine incidentStateMachine = new IncidentStateMachine();

    private WorkOrderService workOrderService;
    private StatusChangeRequestService statusChangeRequestService;
    private IncidentReportService incidentService;
    private AlertService alertService;
    private DashboardService dashboardService;
    private MaintenanceAlertScheduler alertScheduler;

    private Equipment sampleEquipment;
    private AppUser sampleManager;
    private AppUser sampleTechA;
    private AppUser sampleUser;

    @BeforeEach
    void setUp() {
        workOrderService = new WorkOrderService(
                workOrderRepository, assignmentRepository, inspectionResultRepository,
                checklistRepository, equipmentRepository, planRepository,
                incidentRepository, userRepository, acceptanceRepository, adjustmentRepository,
                workOrderStateMachine, equipmentStateMachine, authorizationPolicy,
                historyService, auditService
        );

        statusChangeRequestService = new StatusChangeRequestService(
                statusChangeRequestRepository, equipmentRepository, workOrderRepository,
                assignmentRepository, userRepository, historyService, auditService
        );

        incidentService = new IncidentReportService(
                incidentRepository, equipmentRepository, userRepository,
                workOrderRepository, assignmentRepository, incidentStateMachine,
                authorizationPolicy, historyService, auditService
        );

        alertService = new AlertService(planRepository);
        dashboardService = new DashboardService(equipmentRepository, planRepository, incidentRepository);
        alertScheduler = new MaintenanceAlertScheduler(equipmentRepository, planRepository, auditService);

        sampleEquipment = Equipment.builder()
                .equipmentId(10L)
                .code("EQ-TEST-10")
                .status(EquipmentStatus.ACTIVE)
                .type(EquipmentType.builder().typeId(1L).name("Centrifuge").build())
                .build();

        sampleManager = AppUser.builder().userId(2L).username("MGR01").fullName("Manager One").build();
        sampleTechA = AppUser.builder().userId(4L).username("TECH_A").fullName("Tech A").build();
        sampleUser = AppUser.builder().userId(6L).username("USER01").fullName("User One").build();
    }

 
    @Test
    @DisplayName("Acceptance: 400 khi nghiệm thu FAIL mà không có ghi chú (note)")
    void testAccept_FailRequiresNote() {
        AcceptWorkOrderRequest req = AcceptWorkOrderRequest.builder()
                .decision(AcceptanceDecision.FAIL)
                .note("   ")
                .build();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.acceptWorkOrder(100L, req, 2L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("NOTE_REQUIRED", ex.getCode());
    }

    @Test
    @DisplayName("Acceptance: 409 khi nghiệm thu WO chưa ở trạng thái SUBMITTED")
    void testAccept_RequiresSubmittedStatus() {
        MaintenanceWorkOrder wo = MaintenanceWorkOrder.builder()
                .woId(100L)
                .equipment(sampleEquipment)
                .status(WorkOrderStatus.IN_PROGRESS)
                .build();
        when(workOrderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(wo));

        AcceptWorkOrderRequest req = AcceptWorkOrderRequest.builder()
                .decision(AcceptanceDecision.PASS)
                .build();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.acceptWorkOrder(100L, req, 2L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("NOT_SUBMITTED", ex.getCode());
    }

    @Test
    @DisplayName("Acceptance: PASS đóng WO, cập nhật last_completed_date và next_due_date = today + cycle_days, thiết bị ACTIVE")
    void testAccept_PassClosesWoAndUpdatesPlan() {
        MaintenanceWorkOrder wo = MaintenanceWorkOrder.builder()
                .woId(100L)
                .woCode("WO-20261113-1001")
                .equipment(sampleEquipment)
                .type(WorkOrderType.PERIODIC)
                .status(WorkOrderStatus.SUBMITTED)
                .build();
        when(workOrderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(wo));
        when(workOrderRepository.findById(100L)).thenReturn(Optional.of(wo));
        when(userRepository.findById(2L)).thenReturn(Optional.of(sampleManager));
        when(equipmentRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleEquipment));

        MaintenancePlan plan = MaintenancePlan.builder()
                .planId(1L)
                .equipment(sampleEquipment)
                .cycleDays(90)
                .nextDueDate(LocalDate.now().minusDays(1))
                .build();
        when(planRepository.findByEquipment_EquipmentId(10L)).thenReturn(Optional.of(plan));
        when(checklistRepository.findByEquipmentType_TypeId(anyLong())).thenReturn(List.of());
        when(inspectionResultRepository.findByWorkOrder_WoId(100L)).thenReturn(List.of());
        when(acceptanceRepository.findByWorkOrder_WoIdOrderByAcceptedAtDesc(100L)).thenReturn(List.of());

        AcceptWorkOrderRequest req = AcceptWorkOrderRequest.builder()
                .decision(AcceptanceDecision.PASS)
                .note("Nghiệm thu đạt tiêu chuẩn")
                .build();

        WorkOrderDetailResponse resp = workOrderService.acceptWorkOrder(100L, req, 2L);

        assertEquals(WorkOrderStatus.CLOSED, wo.getStatus());
        assertNotNull(wo.getClosedAt());
        assertEquals(EquipmentStatus.ACTIVE, sampleEquipment.getStatus());
        assertEquals(LocalDate.now(), plan.getLastCompletedDate());
        assertEquals(LocalDate.now().plusDays(90), plan.getNextDueDate());

        verify(acceptanceRepository).save(any(WorkOrderAcceptance.class));
        verify(historyService).record(eq(sampleEquipment), eq(wo), eq("WO_ACCEPT"), any(), eq("CLOSED"), any(), anyString());
    }

    @Test
    @DisplayName("Acceptance: FAIL chuyển WO từ SUBMITTED về IN_PROGRESS")
    void testAccept_FailTransitionsToInProgress() {
        MaintenanceWorkOrder wo = MaintenanceWorkOrder.builder()
                .woId(100L)
                .equipment(sampleEquipment)
                .type(WorkOrderType.PERIODIC)
                .status(WorkOrderStatus.SUBMITTED)
                .build();
        when(workOrderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(wo));
        when(workOrderRepository.findById(100L)).thenReturn(Optional.of(wo));
        when(userRepository.findById(2L)).thenReturn(Optional.of(sampleManager));
        when(equipmentRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleEquipment));
        when(planRepository.findByEquipment_EquipmentId(10L)).thenReturn(Optional.empty());
        when(checklistRepository.findByEquipmentType_TypeId(anyLong())).thenReturn(List.of());
        when(inspectionResultRepository.findByWorkOrder_WoId(100L)).thenReturn(List.of());
        when(acceptanceRepository.findByWorkOrder_WoIdOrderByAcceptedAtDesc(100L)).thenReturn(List.of());

        AcceptWorkOrderRequest req = AcceptWorkOrderRequest.builder()
                .decision(AcceptanceDecision.FAIL)
                .note("Rung lắc bất thường, cần cân bằng lại rotor")
                .build();

        workOrderService.acceptWorkOrder(100L, req, 2L);

        assertEquals(WorkOrderStatus.IN_PROGRESS, wo.getStatus());
        verify(acceptanceRepository).save(any(WorkOrderAcceptance.class));
        verify(historyService).record(eq(sampleEquipment), eq(wo), eq("WO_ACCEPT_FAIL"), any(), eq("IN_PROGRESS"), any(), anyString());
    }

    @Test
    @DisplayName("Adjustments: 409 khi tạo điều chỉnh trên WO chưa CLOSED")
    void testAdjustment_RequiresClosedStatus() {
        MaintenanceWorkOrder wo = MaintenanceWorkOrder.builder()
                .woId(100L)
                .equipment(sampleEquipment)
                .status(WorkOrderStatus.SUBMITTED)
                .build();
        when(workOrderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(wo));

        CreateAdjustmentRequest req = CreateAdjustmentRequest.builder()
                .reason("Bổ sung thông tin linh kiện thay thế")
                .build();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workOrderService.createAdjustment(100L, req, 2L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("STATE_VIOLATION", ex.getCode());
    }

    @Test
    @DisplayName("Adjustments: Thành công trên WO CLOSED, WO gốc không bị sửa đổi trực tiếp")
    void testAdjustment_SuccessOnClosedWo() {
        MaintenanceWorkOrder wo = MaintenanceWorkOrder.builder()
                .woId(100L)
                .equipment(sampleEquipment)
                .status(WorkOrderStatus.CLOSED)
                .build();
        when(workOrderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(wo));
        when(userRepository.findById(2L)).thenReturn(Optional.of(sampleManager));
        when(adjustmentRepository.save(any(WorkOrderAdjustment.class))).thenAnswer(i -> {
            WorkOrderAdjustment a = i.getArgument(0);
            a.setAdjustmentId(55L);
            return a;
        });

        CreateAdjustmentRequest req = CreateAdjustmentRequest.builder()
                .reason("Ghi nhận thêm chi phí phụ tùng chính hãng")
                .changeDetail("{\"cost\": 2500000}")
                .build();

        WorkOrderAdjustmentResponse resp = workOrderService.createAdjustment(100L, req, 2L);

        assertNotNull(resp);
        assertEquals(55L, resp.getAdjustmentId());
        assertEquals("Ghi nhận thêm chi phí phụ tùng chính hãng", resp.getReason());
         verify(workOrderRepository, never()).save(wo);
        verify(adjustmentRepository).save(any(WorkOrderAdjustment.class));
    }

    @Test
    @DisplayName("Adjustments GET: Manager và Technician được giao xem được; Unassigned Technician / Admin / User bị ném 403")
    void testGetAdjustments_Authorization() {
        WorkOrderAdjustment adj = WorkOrderAdjustment.builder()
                .adjustmentId(1L)
                .reason("Bổ sung phụ tùng sau khi đóng WO")
                .changeDetail("{\"replaced\": \"fuse\"}")
                .createdBy(sampleManager)
                .createdAt(Instant.now())
                .build();
        when(adjustmentRepository.findByWorkOrder_WoIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(adj));

        doNothing().when(authorizationPolicy).assertCanReadWorkOrder(100L, 2L, RoleName.MANAGER.name());
        doNothing().when(authorizationPolicy).assertCanReadWorkOrder(100L, 4L, RoleName.TECHNICIAN.name());
        doThrow(new AccessDeniedBusinessException("WO_NOT_ASSIGNED", "Bạn không được phân công"))
                .when(authorizationPolicy).assertCanReadWorkOrder(100L, 5L, RoleName.TECHNICIAN.name());
        doThrow(new AccessDeniedBusinessException("ACCESS_DENIED", "Không đủ quyền"))
                .when(authorizationPolicy).assertCanReadWorkOrder(100L, 1L, RoleName.ADMIN.name());
        doThrow(new AccessDeniedBusinessException("ACCESS_DENIED", "Không đủ quyền"))
                .when(authorizationPolicy).assertCanReadWorkOrder(100L, 6L, RoleName.EQUIPMENT_USER.name());

         List<WorkOrderAdjustmentResponse> mgrResp = workOrderService.getAdjustments(100L, 2L, RoleName.MANAGER.name());
        assertEquals(1, mgrResp.size());
        assertEquals("Bổ sung phụ tùng sau khi đóng WO", mgrResp.get(0).getReason());

         List<WorkOrderAdjustmentResponse> techResp = workOrderService.getAdjustments(100L, 4L, RoleName.TECHNICIAN.name());
        assertEquals(1, techResp.size());

         AccessDeniedBusinessException exTech = assertThrows(AccessDeniedBusinessException.class,
                () -> workOrderService.getAdjustments(100L, 5L, RoleName.TECHNICIAN.name()));
        assertEquals("WO_NOT_ASSIGNED", exTech.getReasonCode());

         AccessDeniedBusinessException exAdmin = assertThrows(AccessDeniedBusinessException.class,
                () -> workOrderService.getAdjustments(100L, 1L, RoleName.ADMIN.name()));
        assertEquals("ACCESS_DENIED", exAdmin.getReasonCode());

         AccessDeniedBusinessException exUser = assertThrows(AccessDeniedBusinessException.class,
                () -> workOrderService.getAdjustments(100L, 6L, RoleName.EQUIPMENT_USER.name()));
        assertEquals("ACCESS_DENIED", exUser.getReasonCode());
    }

 
    @Test
    @DisplayName("OOS: 403 khi Technician không được phân công trên thiết bị đề xuất OOS")
    void testOos_TechnicianNotAssignedThrows403() {
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(sampleEquipment));
        when(assignmentRepository.existsByWorkOrder_Equipment_EquipmentIdAndTechnician_UserId(10L, 4L)).thenReturn(false);

        CreateStatusRequestDto req = CreateStatusRequestDto.builder()
                .proposedStatus("OUT_OF_SERVICE")
                .reason("Hỏng bạc đạn gây tiếng ồn lớn")
                .build();

        AccessDeniedBusinessException ex = assertThrows(AccessDeniedBusinessException.class,
                () -> statusChangeRequestService.proposeOutOfService(10L, req, 4L));
        assertEquals("NOT_ASSIGNED", ex.getReasonCode());
    }

    @Test
    @DisplayName("OOS: 409 khi đã có đề xuất PENDING đang chờ xử lý cho thiết bị")
    void testOos_DuplicatePendingProposalThrows409() {
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(sampleEquipment));
        when(assignmentRepository.existsByWorkOrder_Equipment_EquipmentIdAndTechnician_UserId(10L, 4L)).thenReturn(true);
        when(statusChangeRequestRepository.existsByEquipment_EquipmentIdAndStatus(10L, StatusRequestStatus.PENDING)).thenReturn(true);

        CreateStatusRequestDto req = CreateStatusRequestDto.builder()
                .proposedStatus("OUT_OF_SERVICE")
                .reason("Cần ngưng máy để đại tu")
                .build();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> statusChangeRequestService.proposeOutOfService(10L, req, 4L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("PENDING_REQUEST_EXISTS", ex.getCode());
    }

    @Test
    @DisplayName("OOS: Đề xuất OOS hợp lệ -> Trạng thái request PENDING, thiết bị GIỮ NGUYÊN trạng thái cũ")
    void testOos_ProposalLeavesEquipmentUnchanged() {
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(sampleEquipment));
        when(assignmentRepository.existsByWorkOrder_Equipment_EquipmentIdAndTechnician_UserId(10L, 4L)).thenReturn(true);
        when(statusChangeRequestRepository.existsByEquipment_EquipmentIdAndStatus(10L, StatusRequestStatus.PENDING)).thenReturn(false);
        when(userRepository.findById(4L)).thenReturn(Optional.of(sampleTechA));
        when(statusChangeRequestRepository.save(any(StatusChangeRequest.class))).thenAnswer(i -> {
            StatusChangeRequest r = i.getArgument(0);
            r.setRequestId(12L);
            return r;
        });

        CreateStatusRequestDto req = CreateStatusRequestDto.builder()
                .proposedStatus("OUT_OF_SERVICE")
                .reason("Cháy cuộn dây động cơ")
                .build();

        StatusChangeRequestResponse resp = statusChangeRequestService.proposeOutOfService(10L, req, 4L);

        assertNotNull(resp);
        assertEquals(StatusRequestStatus.PENDING, resp.getStatus());
        assertEquals(EquipmentStatus.ACTIVE, sampleEquipment.getStatus());  
        verify(equipmentRepository, never()).save(sampleEquipment);
    }

    @Test
    @DisplayName("OOS: Manager CONFIRM -> Request CONFIRMED, thiết bị chuyển thành OUT_OF_SERVICE")
    void testOos_ManagerConfirmSetsOutOfService() {
        StatusChangeRequest request = StatusChangeRequest.builder()
                .requestId(12L)
                .equipment(sampleEquipment)
                .status(StatusRequestStatus.PENDING)
                .proposedStatus("OUT_OF_SERVICE")
                .reason("Cháy cuộn dây động cơ")
                .build();

        when(statusChangeRequestRepository.findByIdWithLock(12L)).thenReturn(Optional.of(request));
        when(userRepository.findById(2L)).thenReturn(Optional.of(sampleManager));
        when(equipmentRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleEquipment));

        StatusRequestDecisionDto dec = StatusRequestDecisionDto.builder()
                .decision("CONFIRM")
                .note("Đã xác minh cháy động cơ, ngưng thiết bị để sửa chữa")
                .build();

        StatusChangeRequestResponse resp = statusChangeRequestService.decideStatusRequest(12L, dec, 2L);

        assertEquals(StatusRequestStatus.CONFIRMED, resp.getStatus());
        assertEquals(EquipmentStatus.OUT_OF_SERVICE, sampleEquipment.getStatus());
        verify(equipmentRepository).save(sampleEquipment);
    }

    @Test
    @DisplayName("OOS: Manager REJECT -> Request REJECTED, thiết bị giữ nguyên trạng thái")
    void testOos_ManagerRejectLeavesEquipmentUnchanged() {
        StatusChangeRequest request = StatusChangeRequest.builder()
                .requestId(12L)
                .equipment(sampleEquipment)
                .status(StatusRequestStatus.PENDING)
                .proposedStatus("OUT_OF_SERVICE")
                .reason("Cháy cuộn dây động cơ")
                .build();

        when(statusChangeRequestRepository.findByIdWithLock(12L)).thenReturn(Optional.of(request));
        when(userRepository.findById(2L)).thenReturn(Optional.of(sampleManager));
        when(equipmentRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleEquipment));

        StatusRequestDecisionDto dec = StatusRequestDecisionDto.builder()
                .decision("REJECT")
                .note("Vẫn vận hành được ở tải thấp, chưa cần OOS")
                .build();

        StatusChangeRequestResponse resp = statusChangeRequestService.decideStatusRequest(12L, dec, 2L);

        assertEquals(StatusRequestStatus.REJECTED, resp.getStatus());
        assertEquals(EquipmentStatus.ACTIVE, sampleEquipment.getStatus());
        verify(equipmentRepository, never()).save(sampleEquipment);
    }

 
    @Test
    @DisplayName("Incidents: Báo sự cố tạo mã tracking INC-YYYYMMDD-XXXX và trạng thái REPORTED")
    void testIncident_CreateReported() {
        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(sampleEquipment));
        when(userRepository.findById(6L)).thenReturn(Optional.of(sampleUser));
        when(incidentRepository.save(any(IncidentReport.class))).thenAnswer(i -> {
            IncidentReport inc = i.getArgument(0);
            inc.setIncidentId(99L);
            return inc;
        });

        IncidentCreateRequest req = IncidentCreateRequest.builder()
                .equipmentId(10L)
                .description("Máy phát ra tiếng rít lớn khi khởi động")
                .build();

        IncidentResponse resp = incidentService.createIncident(req, 6L);

        assertNotNull(resp);
        assertEquals(99L, resp.getIncidentId());
        assertTrue(resp.getTrackingCode().startsWith("INC-"));
        assertEquals(IncidentStatus.REPORTED, resp.getStatus());
    }

    @Test
    @DisplayName("Incidents: Phân loại sự cố chuyển từ REPORTED sang CLASSIFIED")
    void testIncident_ClassifySuccess() {
        IncidentReport incident = IncidentReport.builder()
                .incidentId(99L)
                .equipment(sampleEquipment)
                .status(IncidentStatus.REPORTED)
                .build();
        when(incidentRepository.findByIdWithLock(99L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(IncidentReport.class))).thenAnswer(i -> i.getArgument(0));

        ClassifyIncidentRequest req = ClassifyIncidentRequest.builder()
                .severity(IncidentSeverity.HIGH)
                .build();

        IncidentResponse resp = incidentService.classifyIncident(99L, req, 2L);

        assertEquals(IncidentStatus.CLASSIFIED, resp.getStatus());
        assertEquals(IncidentSeverity.HIGH, resp.getSeverity());
    }

    @Test
    @DisplayName("Incidents: 409 khi phân loại sự cố không ở trạng thái REPORTED")
    void testIncident_ClassifyNonReportedThrows409() {
        IncidentReport incident = IncidentReport.builder()
                .incidentId(99L)
                .equipment(sampleEquipment)
                .status(IncidentStatus.ASSIGNED)
                .build();
        when(incidentRepository.findByIdWithLock(99L)).thenReturn(Optional.of(incident));

        ClassifyIncidentRequest req = ClassifyIncidentRequest.builder()
                .severity(IncidentSeverity.HIGH)
                .build();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> incidentService.classifyIncident(99L, req, 2L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("STATE_VIOLATION", ex.getCode());
    }

    @Test
    @DisplayName("Incidents: Technician chuyển tiến độ RESOLVED chỉ khi Repair WO đã SUBMITTED")
    void testIncident_ProgressToResolvedRequiresSubmittedRepairWo() {
        IncidentReport incident = IncidentReport.builder()
                .incidentId(99L)
                .equipment(sampleEquipment)
                .status(IncidentStatus.IN_REPAIR)
                .build();
        when(incidentRepository.findByIdWithLock(99L)).thenReturn(Optional.of(incident));
        when(assignmentRepository.existsByWorkOrder_Incident_IncidentIdAndTechnician_UserId(99L, 4L)).thenReturn(true);

        MaintenanceWorkOrder repairWo = MaintenanceWorkOrder.builder()
                .woId(200L)
                .status(WorkOrderStatus.IN_PROGRESS)  
                .build();
        when(workOrderRepository.findFirstByIncident_IncidentIdOrderByCreatedAtDesc(99L)).thenReturn(Optional.of(repairWo));

        IncidentProgressRequest req = IncidentProgressRequest.builder()
                .status(IncidentStatus.RESOLVED)
                .note("Đã thay xong linh kiện")
                .build();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> incidentService.updateProgress(99L, req, 4L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("STATE_VIOLATION", ex.getCode());
    }

    @Test
    @DisplayName("Incidents: Manager đóng sự cố chỉ khi RESOLVED và Repair WO đã CLOSED")
    void testIncident_CloseRequiresResolvedAndClosedWo() {
        IncidentReport incident = IncidentReport.builder()
                .incidentId(99L)
                .equipment(sampleEquipment)
                .status(IncidentStatus.RESOLVED)
                .build();
        when(incidentRepository.findByIdWithLock(99L)).thenReturn(Optional.of(incident));

        MaintenanceWorkOrder repairWo = MaintenanceWorkOrder.builder()
                .woId(200L)
                .status(WorkOrderStatus.SUBMITTED)  
                .build();
        when(workOrderRepository.findFirstByIncident_IncidentIdOrderByCreatedAtDesc(99L)).thenReturn(Optional.of(repairWo));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> incidentService.closeIncident(99L, 2L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("STATE_VIOLATION", ex.getCode());
    }

 
    @Test
    @DisplayName("Alerts & Dashboard: Tính toán số lượng quá hạn, đến hạn và sự cố mở chính xác")
    void testAlertsAndDashboard() {
        LocalDate today = LocalDate.now();

        MaintenancePlan planOverdue = MaintenancePlan.builder()
                .planId(1L)
                .equipment(sampleEquipment)
                .nextDueDate(today.minusDays(2))
                .build();

        MaintenancePlan planDueSoon = MaintenancePlan.builder()
                .planId(2L)
                .equipment(sampleEquipment)
                .nextDueDate(today.plusDays(3))
                .build();

        MaintenancePlan planNormal = MaintenancePlan.builder()
                .planId(3L)
                .equipment(sampleEquipment)
                .nextDueDate(today.plusDays(40))
                .build();

        when(planRepository.findAll()).thenReturn(List.of(planOverdue, planDueSoon, planNormal));
        when(equipmentRepository.countByStatus(EquipmentStatus.ACTIVE)).thenReturn(8L);
        when(equipmentRepository.countByStatus(EquipmentStatus.UNDER_MAINTENANCE)).thenReturn(1L);
        when(equipmentRepository.countByStatus(EquipmentStatus.OUT_OF_SERVICE)).thenReturn(1L);
        when(incidentRepository.countByStatusNot(IncidentStatus.CLOSED)).thenReturn(3L);

        List<AlertResponse> alerts = alertService.getAlerts();
        assertEquals(2, alerts.size());  

        DashboardResponse dash = dashboardService.getDashboard();
        assertEquals(1, dash.getOverdueCount());
        assertEquals(1, dash.getDueSoonCount());
        assertEquals(3, dash.getOpenIncidents());
        assertEquals(8L, dash.getEquipmentByStatus().get(EquipmentStatus.ACTIVE));
    }
}

